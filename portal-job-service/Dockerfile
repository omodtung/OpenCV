FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY job-domain/pom.xml job-domain/
COPY job-app/pom.xml job-app/
COPY job-rest/pom.xml job-rest/
COPY job-jms/pom.xml job-jms/
COPY job-sqs/pom.xml job-sqs/
COPY job-start/pom.xml job-start/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B -pl job-start -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/job-start/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]