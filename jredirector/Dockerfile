#FROM  maven:3.9.16-eclipse-temurin-21-noble AS builder
FROM  maven:3.9.16-eclipse-temurin-26-noble AS builder

WORKDIR /src
COPY pom.xml .
COPY src src

RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:26-jdk

WORKDIR /app

COPY --from=builder /src/target/jredirector-0.0.1-SNAPSHOT.jar jredirector.jar

ENV PORT=3001
ENV DATABASE_URL="jdbc:postgresql://postgres:postgres@db:5432/shortener"
ENV REDIS_URL="redis://redis:6379"

EXPOSE ${PORT}

SHELL [ "/bin/sh", "-c" ]
ENTRYPOINT java -XX:+EnableDynamicAgentLoading -jar jredirector.jar
