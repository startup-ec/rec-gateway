# ==============================
# ETAPA 1: BUILD (Maven)
# ==============================
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copiamos solo lo necesario primero (cache)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el código
COPY src ./src

# Construimos el JAR
RUN mvn clean package -DskipTests

# ==============================
# ETAPA 2: RUNTIME (Lightweight)
# ==============================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Render expone el puerto por variable PORT
EXPOSE 8080

# Copiamos el JAR desde el build
COPY --from=build /app/target/*.jar app.jar

# JVM optimizada para contenedores pequeños
ENV JAVA_OPTS="\
-XX:+UseContainerSupport \
-XX:MaxRAMPercentage=75 \
-XX:InitialRAMPercentage=50 \
-XX:+ExitOnOutOfMemoryError \
-Djava.security.egd=file:/dev/./urandom"

# Arranque
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
