FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY employer-domain/pom.xml employer-domain/
COPY employer-app/pom.xml employer-app/
COPY employer-rest/pom.xml employer-rest/
COPY employer-jms/pom.xml employer-jms/
COPY employer-sqs/pom.xml employer-sqs/
COPY employer-start/pom.xml employer-start/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B -pl employer-start -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/employer-start/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]