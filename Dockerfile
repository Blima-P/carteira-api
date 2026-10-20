# syntax=docker/dockerfile:1

# ---- Build: compila o jar e separa as camadas do Spring Boot ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Dependências primeiro: enquanto o pom.xml não mudar, esta camada vem do cache
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline

# Os testes já rodam no CI (contra um PostgreSQL real); aqui só empacota
COPY src/main/ src/main/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp package -Dmaven.test.skip=true \
    && cp target/carteira-api-*.jar application.jar \
    && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Runtime: só o JRE e a aplicação, sem Maven nem código-fonte ----
FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.source="https://github.com/Blima-P/carteira-api" \
      org.opencontainers.image.description="API REST de carteira digital" \
      org.opencontainers.image.licenses="MIT"

# Usuário sem privilégios, com UID numérico (exigido por políticas como runAsNonRoot do Kubernetes)
RUN addgroup -S -g 10001 carteira && adduser -S -u 10001 -G carteira carteira
WORKDIR /app

# Da camada que menos muda para a que mais muda: uma nova versão do código só troca a última
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

USER 10001:10001
EXPOSE 8080

# Usa até 75% da memória do contêiner, em vez de 25% (padrão da JVM)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "application.jar"]
