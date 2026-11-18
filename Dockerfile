# syntax=docker/dockerfile:1

# ---------- Build stage ----------
# The backend is a Maven multi-module reactor, so the whole repository is the build
# context and the server fat jar is produced by the Spring Boot plugin.
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY saas-wq-common/pom.xml saas-wq-common/
COPY saas-wq-framework/pom.xml saas-wq-framework/
COPY saas-wq-server/pom.xml saas-wq-server/
COPY saas-wq-web/pom.xml saas-wq-web/

COPY saas-wq-common/src saas-wq-common/src
COPY saas-wq-framework/src saas-wq-framework/src
COPY saas-wq-server/src saas-wq-server/src

RUN mvn -B -DskipTests clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --from=build /workspace/saas-wq-server/target/saas-wq-server.jar /app/app.jar

RUN chown -R app:app /app

USER app

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
