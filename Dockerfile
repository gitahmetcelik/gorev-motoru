FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

COPY pom.xml .
COPY motor-cekirdek/pom.xml motor-cekirdek/
COPY motor-spring-starter/pom.xml motor-spring-starter/
COPY motor-api/pom.xml motor-api/
RUN mvn -q -B dependency:go-offline -pl motor-api -am

COPY motor-cekirdek/src motor-cekirdek/src
COPY motor-spring-starter/src motor-spring-starter/src
COPY motor-api/src motor-api/src
RUN mvn -q -B package -pl motor-api -am -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/motor-api/target/motor-api-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
