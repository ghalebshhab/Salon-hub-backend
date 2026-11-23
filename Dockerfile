# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copy Maven wrapper and pom first (layer-cache friendly)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

# Copy source and build the fat JAR
COPY src/ src/
# Copy the executable JAR to a fixed name, so a version change in pom.xml
# never breaks the image (the .jar.original file does not match the pattern).
RUN ./mvnw clean package -DskipTests -q && cp target/backend-*.jar target/app.jar

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/app.jar app.jar

# Render injects $PORT at runtime; default to 8080 locally
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar app.jar"]
