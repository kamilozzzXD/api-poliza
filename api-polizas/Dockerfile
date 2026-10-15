# Etapa 1: Compilacion y empaquetado con Maven y JDK 17
FROM maven:3.9.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copia de descriptor de dependencias para optimizar cache de capas
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia de codigo fuente y compilacion omitiendo pruebas para agilidad del build
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen ligera de ejecucion con JRE 17 Alpine
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copia del artefacto generado desde la etapa de compilacion
COPY --from=build /app/target/*.jar app.jar

# Exposicion del puerto estandar de Spring Boot
EXPOSE 8080

# Configuracion de ejecucion de la aplicacion
ENTRYPOINT ["java", "-jar", "app.jar"]
