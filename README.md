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

- Event Service → consulta disponibilidad de eventos
- Payment Service → confirmación de pagos
- RabbitMQ → mensajería asincrónica
- Redis → manejo de stock de tickets

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

- `PENDING` → Orden creada, esperando pago
- `PAID` → Pago confirmado
- `FAILED` → Pago fallido o error
- `EXPIRED` → Tiempo de pago expirado

---

## Endpoints

### Crear orden

**POST** `/orders`

```json
{
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2
}

```
### Respuesta: 

```json
{
  "id": "uuid",
  "status": "PENDING",
  "createdAt": "2026-01-01T10:00:00",
  "expiresAt": "2026-01-01T10:05:00"
}
```

## Confirmar pago (callback)

POST /orders/payment-confirmation

```json
{
  "orderId": "uuid",
  "success": true,
  "redisKey": "tickets:event:1:type:1",
  "quantity": 2
}
```

### Respuesta: 
```json
200 OK
```

---

## Pruebas (Postman / Thunder Client)
### 1. Crear orden
Método: POST
URL: http://localhost:8080/orders

Body:

```json
{
  "eventId": "1",
  "ticketTypeId": "1",
  "quantity": 2
}
```

### 2. Confirmar pago exitoso

```json
{
  "orderId": "ID_GENERADO",
  "success": true,
  "redisKey": "tickets:event:1:type:1",
  "quantity": 2
}
```

### 3. Confirmar pago fallido
```json
{
  "orderId": "ID_GENERADO",
  "success": false,
  "redisKey": "tickets:event:1:type:1",
  "quantity": 2
}
```

## Redis

Se usa para manejar el stock de tickets:

```json
tickets:event:{eventId}:type:{ticketTypeId}
```

---

## RabbitMQ

### Evento publicado

**Routing Key:** order.created

```json
{
  "orderId": "uuid",
  "eventId": "uuid",
  "ticketTypeId": "uuid",
  "quantity": 2
}
```

## Scheduler

Se ejecuta cada 60 segundos:

Busca órdenes expiradas
Cambia estado a EXPIRED
Libera tickets en Redis

---

## Métricas (Actuator)

Disponibles en:

```json
/actuator/metrics/orders.created
/actuator/metrics/orders.failed
```

---

## Manejo de errores

Se implementa un GlobalExceptionHandler que maneja:

- Errores de negocio → 400
- Validaciones → 400
- Errores internos → 500

---

## 🐳 Docker

El microservicio está dockerizado y puede ejecutarse junto a:

- PostgreSQL
- Redis
- RabbitMQ

---

## ⚠️ Consideraciones

- El microservicio depende del Event Service
- Redis debe estar inicializado con stock
- RabbitMQ debe estar disponible para eventos

---

 ### Autor:

Proyecto desarrollado como parte del sistema VivaEventos

- Juan Alejandri Jimenez Mendoza
- Diego Alejandro Galvis Unas


