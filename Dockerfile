FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN groupadd -r app && useradd -r -g app -s /bin/false app

COPY --from=build --chown=app:app /workspace/target/tenpo-challenge-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
