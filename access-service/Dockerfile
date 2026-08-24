# syntax=docker/dockerfile:1
#
# Standalone build for access-service, for platforms (e.g. Render) whose
# Docker build step doesn't support passing --build-arg into a shared
# Dockerfile. Local dev / docker-compose use the shared root Dockerfile
# instead (one `mvn package` reused across every service image) — see
# ../../Dockerfile.
#
# Build context must stay the repo root (so `shared/` and `pom.xml` are
# visible); only the Dockerfile path changes. On Render: Root Directory = "."
# Dockerfile Path = "services/access-service/Dockerfile".

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY shared shared
COPY services services

RUN mvn -q -pl services/access-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S aegiszero && adduser -S aegiszero -G aegiszero
COPY --from=build /workspace/services/access-service/target/access-service.jar /app/app.jar
USER aegiszero

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
