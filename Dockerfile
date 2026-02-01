# Dockerfile para Microservicios Spring Boot
# Este mismo Dockerfile se usa para TODOS los microservicios
# Colócalo en la carpeta raíz de cada microservicio

# Etapa 1: Construcción
FROM maven:3.9.5-eclipse-temurin-21 AS build
WORKDIR /app

# Copiar pom.xml primero para aprovechar caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y construir
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear usuario no-root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring
# Crear directorio de logs y dar permisos
RUN mkdir -p /app/logs && \
    chown -R spring:spring /app/logs
USER spring:spring

# Copiar el JAR desde la etapa de construcción
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto (se sobreescribe en docker-compose para cada servicio)
EXPOSE 8080

# Configuración de JVM para contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
