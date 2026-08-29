# ============================================================
# STAGE 1: Build the application
# ============================================================
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies separately for faster rebuilds
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# ============================================================
# STAGE 2: Run the application
# ============================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Run as a non-root user for security (principle of least privilege)
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p logs && chown -R spring:spring logs
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]