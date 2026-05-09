FROM openjdk:17-jdk-alpine
COPY target/ontobot-1.0-SNAPSHOT.jar app.jar
COPY .env.docker .env
ENTRYPOINT ["java","-jar","/app.jar"]