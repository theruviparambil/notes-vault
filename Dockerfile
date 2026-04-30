# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline -DskipTests

COPY src src
RUN ./mvnw -B -ntp -DskipTests package && \
    java -Djarmode=tools -jar target/notes-vault-*.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app && \
    apk add --no-cache wget

COPY --from=build --chown=app:app /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=app:app /workspace/target/extracted/application/ ./

USER app
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
