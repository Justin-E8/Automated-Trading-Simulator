FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .
# This project uses an older mvnw script that forwards MAVEN_CONFIG as a CLI arg.
# Maven base images set MAVEN_CONFIG=/root/.m2 by default, so clear it here.
RUN MAVEN_CONFIG="" ./mvnw -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/target/automated-trading-simulator-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
