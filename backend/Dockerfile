# ---------- Build Stage ----------
FROM maven:3.9.8-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ---------- Runtime Stage ----------
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Create persistence directory
RUN mkdir -p data

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]