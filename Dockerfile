FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .
# Ensure wrapper invocation treats only explicit Maven goals as CLI arguments.
RUN MAVEN_CONFIG="" ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/target/automated-trading-simulator-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
