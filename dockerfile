# Imagen base
FROM eclipse-temurin:21-jdk

# Carpeta interna
WORKDIR /app

# Copiar jar
COPY target/order-0.0.1-SNAPSHOT.jar app.jar

# Exponer puerto
EXPOSE 8080

# Ejecutar app
ENTRYPOINT ["java", "-jar", "app.jar"]