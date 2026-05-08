FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

ARG MAVEN_PROFILE=ollama

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -P${MAVEN_PROFILE} -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -P${MAVEN_PROFILE} -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/personal-finance-service-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
