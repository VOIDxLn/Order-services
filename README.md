# Order Service - VivaEventos

Microservicio encargado de la gestión de órdenes de compra dentro del sistema VivaEventos.  
Permite crear órdenes, reservar tickets, manejar expiraciones y procesar confirmaciones de pago.

---

## Tecnologías utilizadas

- Java 21
- Spring Boot 3.3.5
- Spring Data JPA
- PostgreSQL
- Redis
- RabbitMQ
- Lombok
- Spring Validation (Jakarta)
- Spring Boot Actuator
- Micrometer (métricas)
- Docker

---

## Arquitectura

Este microservicio forma parte de una arquitectura de microservicios y se comunica con:

- **Event Service** → consulta disponibilidad de eventos
- **Payment Service** → confirmación de pagos
- **RabbitMQ** → mensajería asincrónica
- **Redis** → manejo de stock de tickets

---

## Funcionalidades principales

### Crear orden
- Valida disponibilidad de tickets
- Reserva cupos en Redis
- Guarda la orden en base de datos
- Publica evento en RabbitMQ

### Expiración automática
- Scheduler que expira órdenes cada 60 segundos
- Libera cupos en Redis

### Confirmación de pago
- Maneja callbacks o eventos
- Actualiza estado de la orden
- Libera cupos si el pago falla

### Observabilidad básica
- Logging estructurado
- Métricas con Micrometer:
  - `orders.created`
  - `orders.failed`

---

## Estados de una orden

| Estado | Descripción |
|--------|-------------|
| `PENDING` | Orden creada, esperando pago |
| `PAID` | Pago confirmado |
| `FAILED` | Pago fallido o error |
| `EXPIRED` | Tiempo de pago expirado |

---

## Endpoints

### Crear orden
**POST** `/orders`
```json
{
  "eventId": "11111111-1111-1111-1111-111111111111",
  "ticketTypeId": "22222222-2222-2222-2222-222222222222",
  "quantity": 2
}
```
**Respuesta exitosa (200):**
```json
{
  "id": "uuid-generado",
  "eventId": "11111111-1111-1111-1111-111111111111",
  "ticketTypeId": "22222222-2222-2222-2222-222222222222",
  "quantity": 2,
  "status": "PENDING",
  "createdAt": "2026-01-01T10:00:00",
  "expiresAt": "2026-01-01T10:05:00"
}
```

### Obtener orden por ID
**GET** `/orders/{id}`

**Respuesta exitosa (200):**
```json
{
  "id": "uuid",
  "eventId": "11111111-1111-1111-1111-111111111111",
  "ticketTypeId": "22222222-2222-2222-2222-222222222222",
  "quantity": 2,
  "status": "PENDING",
  "createdAt": "2026-01-01T10:00:00",
  "expiresAt": "2026-01-01T10:05:00"
}
```

### Confirmar pago
**POST** `/orders/payment-confirmation`
```json
{
  "orderId": "uuid",
  "success": true
}
```
**Respuesta exitosa:** `200 OK` (sin body)

---

## Pruebas paso a paso (Thunder Client / Postman)

> Sigue este flujo en orden para probar el microservicio correctamente.

### Paso 0 — Levantar los contenedores

```bash
docker-compose down -v
mvn clean package -DskipTests
docker-compose build --no-cache
docker-compose up
```

Espera a que todos los servicios estén listos. Verifica en los logs que aparezca:
```
Started OrderApplication in X seconds
```

### Paso 1 — Inicializar cupos en Redis

> ⚠️ Este paso es obligatorio cada vez que se levanten los contenedores desde cero.  
> Redis no persiste datos entre reinicios con `docker-compose down -v`.

Abre una terminal y ejecuta:

```bash
docker exec -it order-redis redis-cli
```

Dentro del CLI de Redis, ejecuta:

```
SET tickets:event:11111111-1111-1111-1111-111111111111:type:22222222-2222-2222-2222-222222222222 100
```

Debe responder `OK`. Luego sal con `exit`.

> El número `100` representa los cupos disponibles para ese evento y tipo de ticket.

### Paso 2 — Crear una orden

**Método:** `POST`  
**URL:** `http://localhost:8080/orders`  
**Headers:** `Content-Type: application/json`  
**Body:**
```json
{
  "eventId": "11111111-1111-1111-1111-111111111111",
  "ticketTypeId": "22222222-2222-2222-2222-222222222222",
  "quantity": 2
}
```

**Respuesta esperada (200):**
```json
{
  "id": "dd41786a-20be-4c9c-a028-827806e9dadd",
  "eventId": "11111111-1111-1111-1111-111111111111",
  "ticketTypeId": "22222222-2222-2222-2222-222222222222",
  "quantity": 2,
  "status": "PENDING",
  "createdAt": "2026-05-16T05:14:06.986028206",
  "expiresAt": "2026-05-16T05:19:06.986041142"
}
```

> Copia el valor del campo `id` — lo necesitas para los siguientes pasos.

### Paso 3 — Consultar la orden creada

**Método:** `GET`  
**URL:** `http://localhost:8080/orders/{id}`  
> Reemplaza `{id}` con el UUID del paso anterior, por ejemplo:  
> `http://localhost:8080/orders/dd41786a-20be-4c9c-a028-827806e9dadd`

**Respuesta esperada (200):**
```json
{
  "id": "dd41786a-20be-4c9c-a028-827806e9dadd",
  "status": "PENDING",
  ...
}
```

### Paso 4 — Confirmar pago exitoso

**Método:** `POST`  
**URL:** `http://localhost:8080/orders/payment-confirmation`  
**Headers:** `Content-Type: application/json`  
**Body:**
```json
{
  "orderId": "dd41786a-20be-4c9c-a028-827806e9dadd",
  "success": true
}
```

**Respuesta esperada:** `200 OK` (sin body)

### Paso 5 — Verificar que la orden cambió a PAID

**Método:** `GET`  
**URL:** `http://localhost:8080/orders/dd41786a-20be-4c9c-a028-827806e9dadd`

**Respuesta esperada (200):**
```json
{
  "id": "dd41786a-20be-4c9c-a028-827806e9dadd",
  "status": "PAID",
  ...
}
```

### Paso 6 — Probar pago fallido (opcional)

Crea una nueva orden (repite el Paso 2), copia el nuevo `id` y envía:

**Método:** `POST`  
**URL:** `http://localhost:8080/orders/payment-confirmation`  
**Body:**
```json
{
  "orderId": "nuevo-uuid-generado",
  "success": false
}
```

Luego consulta la orden con GET — debe mostrar `"status": "FAILED"` y los cupos deben haberse liberado en Redis.

---

## Redis

Se usa para manejar el stock de tickets en tiempo real con baja latencia.

**Formato de la clave:**
```
tickets:event:{eventId}:type:{ticketTypeId}
```

**Comandos útiles:**
```bash
# Ver cupos actuales de un evento
docker exec -it order-redis redis-cli GET tickets:event:11111111-1111-1111-1111-111111111111:type:22222222-2222-2222-2222-222222222222

# Inicializar cupos manualmente
docker exec -it order-redis redis-cli SET tickets:event:11111111-1111-1111-1111-111111111111:type:22222222-2222-2222-2222-222222222222 100
```

---

## RabbitMQ

### Colas declaradas

| Cola | Descripción |
|------|-------------|
| `order.created.queue` | Recibe eventos cuando se crea una orden |
| `payment.result.queue` | Recibe resultados de pago del Payment Service |

### Exchanges declarados

| Exchange | Tipo | Descripción |
|----------|------|-------------|
| `order.exchange` | direct | Publica eventos de órdenes |
| `payment.exchange` | direct | Recibe resultados de pago |

### Evento publicado al crear una orden
**Exchange:** `order.exchange`  
**Routing Key:** `order.created`
```json
{
  "orderId": "uuid",
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2
}
```

### Verificar en RabbitMQ Management
Accede a `http://localhost:15672` (usuario: `guest`, contraseña: `guest`) para ver las colas y mensajes en tiempo real.

---

## Scheduler

Se ejecuta cada 60 segundos y realiza lo siguiente:
- Busca órdenes con estado `PENDING` cuyo `expiresAt` ya pasó
- Cambia su estado a `EXPIRED`
- Libera los tickets reservados en Redis

---

## Métricas (Actuator)

Disponibles en:
```
GET /actuator/metrics/orders.created
GET /actuator/metrics/orders.failed
GET /actuator/health
```

---

## Manejo de errores

Se implementa un `GlobalExceptionHandler` que retorna respuestas estructuradas:

```json
{
  "error": "Mensaje descriptivo",
  "status": 400,
  "timestamp": "2026-05-16T05:00:00"
}
```

| Código | Causa |
|--------|-------|
| `400` | Errores de negocio o validación |
| `500` | Errores internos del servidor |

---

## 🐳 Docker

El microservicio está dockerizado junto a sus dependencias.

**Servicios incluidos en docker-compose:**

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| `order-service` | 8080 | Microservicio principal |
| `order-postgres` | 5432 | Base de datos PostgreSQL |
| `order-redis` | 6379 | Cache y stock de tickets |
| `order-rabbit` | 5672 / 15672 | Mensajería y panel de administración |

**Comandos útiles:**
```bash
# Levantar todo desde cero
docker-compose down -v
mvn clean package -DskipTests
docker-compose build --no-cache
docker-compose up

# Ver logs del servicio
docker logs order-service -f

# Ver logs de todos los servicios
docker-compose logs -f
```

---

## ⚠️ Consideraciones

- Redis debe inicializarse con stock antes de crear órdenes (ver Paso 1 en Pruebas)
- RabbitMQ debe estar disponible al arrancar el servicio
- Al integrar con Event Service, la inicialización de Redis será automática
- Usar siempre UUIDs válidos en formato `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

---

## Autor

Proyecto desarrollado como parte del sistema **VivaEventos**

- Juan Alejandri Jimenez Mendoza
- Diego Alejandro Galvis Unas
