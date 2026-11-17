# auth-service · TicketYa — build multi-stage (Java 21)
# EN-10 (E2, criterio 2): cada servicio tiene su contenedor; la configuración
# (claves JWT, contraseña del admin) se inyecta por variables de entorno y no
# hay secretos en el repositorio.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependencias
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuario no privilegiado
RUN useradd --system --uid 1001 ticketya
USER ticketya

COPY --from=build /app/target/auth-service-*.jar app.jar

EXPOSE 8081

# Las claves JWT y la contraseña del admin se inyectan por entorno:
#   AUTH_JWT_PRIVATE_KEY / AUTH_JWT_PUBLIC_KEY / AUTH_ADMIN_PASSWORD
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
