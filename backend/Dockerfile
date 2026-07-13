# syntax=docker/dockerfile:1

# ============================================================
# ERP TopPower — imagem do backend Spring Boot
# Build multi-stage:
#   1) compila com Maven + JDK 17 -> gera o JAR em target/
#   2) roda apenas com JRE 17 (imagem final menor)
# ============================================================

# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copia primeiro o pom.xml para baixar dependências em cache
# (camada reutilizada enquanto o pom não mudar).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copia o resto do código-fonte e compila (pula testes; rode localmente).
COPY src ./src

# Empacota pulando os testes.
RUN mvn -B -q clean package -DskipTests

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre

WORKDIR /app

# Diretório de uploads de logos (montado como volume no compose).
RUN mkdir -p /app/uploads

# Copia o JAR construído no stage anterior.
COPY --from=build /app/target/*.jar app.jar

# Variáveis default (podem ser sobrescritas pelo docker-compose).
ENV SERVER_PORT=8081 \
    APP_UPLOADS_DIR=/app/uploads

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]