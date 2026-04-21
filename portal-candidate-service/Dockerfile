FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY candidate-domain/pom.xml candidate-domain/
COPY candidate-app/pom.xml candidate-app/
COPY candidate-rest/pom.xml candidate-rest/
COPY candidate-jms/pom.xml candidate-jms/
COPY candidate-sqs/pom.xml candidate-sqs/
COPY candidate-start/pom.xml candidate-start/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B -pl candidate-start -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/candidate-start/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]