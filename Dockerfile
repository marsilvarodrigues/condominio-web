# syntax=docker/dockerfile:1.7
# A diretiva acima ativa funcionalidades do BuildKit (cache mounts, heredocs).
# Requer Docker 23+ (BuildKit habilitado por padrão) ou DOCKER_BUILDKIT=1.

# ── Build ─────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml ./
# Cache mount: ~/.m2 é reutilizado entre builds pelo BuildKit.
# Sem isso, cada rebuild baixa ~300 MB do Maven Central do zero.
# "sharing=locked" garante que builds paralelos não corrompam o cache.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn dependency:go-offline -q
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn package -DskipTests -q

# ── Extrai Spring Boot layers ──────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /app
COPY --from=build /app/target/condominio-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ── Runtime ───────────────────────────────────────────────────────────────────
# eclipse-temurin:21-jre-jammy (Ubuntu 22.04 LTS, glibc) em vez de Alpine.
# Alpine usa musl libc, que NÃO suporta ZGC. Com glibc + ZGC Generacional,
# as pausas GC ficam abaixo de 1 ms — fundamental com virtual threads Java 21.
# Custo: ~100 MB a mais que Alpine; benefício: latência consistente sob carga.
FROM eclipse-temurin:21-jre-jammy AS runtime

# curl: necessário apenas para o HEALTHCHECK abaixo.
# --no-install-recommends e limpeza de lists mantêm a camada mínima.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appgroup && useradd -r -g appgroup -s /sbin/nologin appuser
WORKDIR /app

# Camadas em ordem crescente de frequência de mudança:
# dependencies e spring-boot-loader raramente mudam → invalidam menos o cache
COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./

RUN chown -R appuser:appgroup /app
USER appuser
EXPOSE 8080

# start-period=60s: aguarda o Spring Boot + Liquibase inicializarem antes de
# considerar o container unhealthy. Ajuste conforme o tempo de startup real.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/api/actuator/health \
        | grep -q '"status":"UP"' || exit 1

# ── JVM tuning ────────────────────────────────────────────────────────────────
# Heap   : inicia em 50% e cresce até 75% do memory limit do container.
#          25% reservados para: metaspace, code cache, buffers off-heap do
#          Undertow (direct-buffers=true, 16 KB × nº de requests ativos)
#          e stacks de threads nativas.
#
# GC     : ZGC Generacional (Java 21+) — pausas sub-milissegundo mesmo sob
#          alta alocação. Contrasta com G1GC que pode pausar 100-200 ms em
#          heaps grandes. ZUncommitDelay=30 devolve heap ao OS após 30 s de
#          ociosidade — evita reserva permanente em pods K8s com HPA.
#          Requer glibc (Jammy); NÃO disponível em Alpine (musl libc).
#
# VTs    : Com spring.threads.virtual.enabled=true, os worker threads do
#          Undertow são virtual threads — pilha expandível gerenciada no heap,
#          não limitada por -Xss. Apenas os IO threads do Undertow (~4 threads
#          de OS) usam a pilha tradicional: 512k é suficiente para o stack
#          depth do Undertow + Spring Security + Hibernate.
#
# OOM    : ExitOnOutOfMemoryError termina imediatamente o processo corrompido
#          em vez de entrar em estado degradado — o orchestrator reinicia.
#
# Override em runtime sem rebuild:
#   docker run -e JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60.0 ..." ...
ENV JAVA_TOOL_OPTIONS="\
  -XX:+UseContainerSupport \
  -XX:InitialRAMPercentage=50.0 \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -XX:ZUncommitDelay=30 \
  -Xss512k \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom \
  -Djava.awt.headless=true"

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
