FROM  maven:3.9.16-eclipse-temurin-26-noble AS builder

WORKDIR /src
COPY pom.xml .
COPY src src

RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:26.0.1_8-jre-noble

WORKDIR /app

COPY --from=builder /src/target/jredirector-0.0.1-SNAPSHOT.jar jredirector.jar

ENV PORT=8080
ENV DATABASE_URL="postgres://postgres:postgres@db:5432/shortener"
ENV REDIS_URL="redis://redis:6379"

EXPOSE ${PORT}

SHELL [ "/bin/sh", "-c" ]
ENTRYPOINT java -jar jredirector.jar
