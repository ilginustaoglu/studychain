# 1. Aşama: Uygulamayı paketleme (Build Stage)
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

# 2. Aşama: Çalıştırma (Runtime Stage)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/studychain-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]