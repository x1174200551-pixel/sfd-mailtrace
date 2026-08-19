# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY deploy/docker/maven-settings.xml /usr/share/maven/ref/settings-docker.xml
COPY backend/pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -s /usr/share/maven/ref/settings-docker.xml -q -DskipTests dependency:go-offline

COPY backend/src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -s /usr/share/maven/ref/settings-docker.xml -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ARG ALPINE_MIRROR=https://mirrors.aliyun.com/alpine
RUN sed -i "s#https://dl-cdn.alpinelinux.org/alpine#${ALPINE_MIRROR}#g" /etc/apk/repositories \
    && apk add --no-cache curl tzdata \
    && addgroup -S mailtrace \
    && adduser -S mailtrace -G mailtrace

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=test

COPY --from=build /app/target/*.jar app.jar
RUN chown -R mailtrace:mailtrace /app

USER mailtrace
EXPOSE 8001

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8001/api/v1/system/health >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
