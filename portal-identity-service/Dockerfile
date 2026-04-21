FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY identity-domain/pom.xml identity-domain/
COPY identity-app/pom.xml identity-app/
COPY identity-rest/pom.xml identity-rest/
COPY identity-jms/pom.xml identity-jms/
COPY identity-sqs/pom.xml identity-sqs/
COPY identity-start/pom.xml identity-start/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B -pl identity-start -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/identity-start/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]