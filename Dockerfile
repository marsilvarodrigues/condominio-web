# syntax=docker/dockerfile:1.7
# A diretiva acima ativa funcionalidades do BuildKit (cache mounts, heredocs).
# Requer Docker 23+ (BuildKit habilitado por padrão) ou DOCKER_BUILDKIT=1.
#
# Imagens produzidas por este Dockerfile (use --target para isolar):
#   frontend  → nginx:alpine servindo o React (porta 80)
#   runtime   → Spring Boot (porta 8080)
#
# Build de produção completo (via docker-compose):
#   docker compose build
#
# Build manual de uma imagem específica:
#   docker build --target frontend -t condogest-frontend .
#   docker build --target runtime  -t condogest-backend  .

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 1 — Compilação do React
# ─────────────────────────────────────────────────────────────────────────────
FROM node:20-alpine AS frontend-build
WORKDIR /frontend

# Instala dependências primeiro para maximizar cache de camada.
# A camada node_modules só é recriada quando package-lock.json muda.
COPY frontend/react/package*.json ./
RUN --mount=type=cache,target=/root/.npm,sharing=locked \
    npm ci --prefer-offline --no-audit

COPY frontend/react/ ./
# NODE_ENV=production desativa source maps e minifica o bundle final.
ENV NODE_ENV=production
RUN npm run build

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 2 — Nginx servindo o SPA
# ─────────────────────────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS frontend

# Remove a configuração padrão do nginx (não serve múltiplos virtual hosts).
RUN rm /etc/nginx/conf.d/default.conf

# Cria a configuração principal usando heredoc (BuildKit 1.7).
# Variável BACKEND_HOST pode ser sobrescrita via --build-arg ou env no runtime.
ARG BACKEND_HOST=backend
RUN cat > /etc/nginx/nginx.conf <<'EOF'
# Ajusta automaticamente ao número de cores disponíveis no container.
worker_processes auto;
# Pid em /tmp para suportar execução sem root em ambientes restritos.
pid /tmp/nginx.pid;
error_log /var/log/nginx/error.log warn;

events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

http {
    include      /etc/nginx/mime.types;
    default_type application/octet-stream;

    # Esconde versão do nginx em headers e páginas de erro.
    server_tokens off;

    # Formato de log com duração da requisição para facilitar análise de perf.
    log_format main '$remote_addr [$time_local] "$request" $status '
                    '$body_bytes_sent "$http_referer" rt=$request_time';
    access_log /var/log/nginx/access.log main;

    # Otimizações de I/O: sendfile evita cópias userspace→kernel para arquivos.
    # tcp_nopush acumula pacotes até MTU antes de enviar (reduz overhead de rede).
    sendfile    on;
    tcp_nopush  on;
    tcp_nodelay on;

    # Mantém conexões HTTP/1.1 ativas por 30 s — reduz handshakes repetidos.
    keepalive_timeout 30s;

    # ── Gzip ────────────────────────────────────────────────────────────────────
    # Comprime assets de texto na transmissão.
    # comp_level 6: equilíbrio entre CPU e taxa de compressão (~70 % menor).
    # Não comprime arquivos < 1 KB (overhead > ganho).
    gzip              on;
    gzip_vary         on;
    gzip_proxied      any;
    gzip_comp_level   6;
    gzip_min_length   1024;
    gzip_types
        text/plain
        text/css
        text/javascript
        application/javascript
        application/json
        application/manifest+json
        application/xml
        image/svg+xml
        font/woff
        font/woff2;

    # ── Upload limit (para conciliação bancária via OFX/CSV) ─────────────────────
    client_max_body_size 10m;

    include /etc/nginx/conf.d/*.conf;
}
EOF

# O nginx:alpine processa arquivos em /etc/nginx/templates/ com envsubst
# na inicialização do container, depositando o resultado em /etc/nginx/conf.d/.
# Isso resolve ${BACKEND_HOST} em runtime sem CMD personalizado nem script extra.
# Variáveis nginx ($host, $remote_addr, etc.) são preservadas porque o
# entrypoint oficial usa: envsubst "$(printf '${%s} ' ${!NGINX_ENVSUBST_*} BACKEND_HOST)"
# — ou seja, substitui apenas as variáveis listadas em NGINX_ENVSUBST_OUTPUT_DIR.
RUN mkdir -p /etc/nginx/templates

RUN cat > /etc/nginx/templates/app.conf.template <<'VHOST'
server {
    listen      80;
    server_name _;
    root        /usr/share/nginx/html;
    index       index.html;

    # ── Cabeçalhos de segurança ────────────────────────────────────────────────
    # X-Frame-Options: impede clickjacking em iframes de outros domínios.
    add_header X-Frame-Options         "SAMEORIGIN"                      always;
    # nosniff: bloqueia MIME-type sniffing no browser.
    add_header X-Content-Type-Options  "nosniff"                         always;
    # Ativa proteção XSS legada (navegadores antigos).
    add_header X-XSS-Protection        "1; mode=block"                   always;
    # Envia apenas a origem (sem path) em requisições cross-origin.
    add_header Referrer-Policy         "strict-origin-when-cross-origin" always;
    # Restringe uso de câmera, microfone e geolocalização.
    add_header Permissions-Policy      "camera=(), microphone=(), geolocation=()" always;

    # ── Cache de assets com hash de conteúdo ──────────────────────────────────
    # Vite gera nomes como /assets/index-Bx3k9aF2.js.
    # Hash no nome → URLs novas a cada deploy → cache pode ser infinito (1 ano).
    # "immutable" instrui o browser a nunca revalidar enquanto o cache não expirar.
    location /assets/ {
        expires    1y;
        add_header Cache-Control        "public, immutable";
        add_header X-Content-Type-Options "nosniff" always;
        access_log off;
    }

    # ── index.html nunca cacheado ──────────────────────────────────────────────
    # Garante que o browser sempre busque a versão mais recente do SPA,
    # que por sua vez carrega os assets com hashes atualizados.
    location = /index.html {
        expires -1;
        add_header Cache-Control "no-store, no-cache, must-revalidate, proxy-revalidate";
        add_header Pragma        "no-cache";
    }

    # ── Arquivos estáticos da raiz ─────────────────────────────────────────────
    location = /favicon.svg { expires 7d; add_header Cache-Control "public"; access_log off; }
    location = /robots.txt  { access_log off; }
    location = /sitemap.xml { access_log off; }

    # ── Proxy reverso para a API Spring Boot ──────────────────────────────────
    # ${BACKEND_HOST} é substituído pelo entrypoint do nginx via envsubst.
    # Permite trocar o host do backend sem rebuild:
    #   docker run -e BACKEND_HOST=meu-api condogest-frontend
    location /api/ {
        proxy_pass         http://${BACKEND_HOST}:8080/api/;
        proxy_http_version 1.1;

        # Preserva informações do cliente original no backend.
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Elimina o header Connection antes de repassar ao upstream (hop-by-hop).
        proxy_set_header Connection        "";

        # Timeouts: 60 s cobre respostas lentas (rateio em lote, extrato OFX).
        proxy_connect_timeout 10s;
        proxy_send_timeout    60s;
        proxy_read_timeout    60s;

        # Buffers intermediários: evita modo de passagem direta para respostas
        # grandes e reduz o número de syscalls de escrita.
        proxy_buffering    on;
        proxy_buffer_size  8k;
        proxy_buffers      8 16k;
    }

    # ── SPA fallback ──────────────────────────────────────────────────────────
    # Rotas do React Router que não existem como arquivo físico → index.html.
    # O React Router toma conta da navegação a partir daí.
    location / {
        try_files $uri $uri/ /index.html;
    }
}
VHOST

# Copia o build do React.
COPY --from=frontend-build /frontend/dist /usr/share/nginx/html

# Garante que o usuário nginx (worker) possa ler os arquivos estáticos.
RUN chown -R nginx:nginx /usr/share/nginx/html \
    && chmod -R 755      /usr/share/nginx/html

# Valor padrão de BACKEND_HOST; sobrescrito em runtime via -e ou env: no compose.
ENV BACKEND_HOST=backend

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD wget -qO- http://localhost/index.html | grep -q '<div id="root">' || exit 1

# O entrypoint oficial do nginx:alpine já faz envsubst nos arquivos *.template
# de /etc/nginx/templates/ e em seguida inicia o nginx.
# CMD herdado da imagem base: ["nginx", "-g", "daemon off;"]

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 3 — Compilação do Spring Boot
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn dependency:go-offline -q
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 4 — Extração de layers do Spring Boot
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS layers
WORKDIR /app
COPY --from=build /app/target/condominio-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# ─────────────────────────────────────────────────────────────────────────────
# STAGE 5 — Runtime do Spring Boot
# ─────────────────────────────────────────────────────────────────────────────
# gcr.io/distroless/java21-debian12 usa glibc (Debian 12) sem shell nem utilitários.
# Vantagens sobre eclipse-temurin:21-jre-jammy: ~530 MB menor, superfície de ataque
# drasticamente reduzida (sem apt, bash, adduser). Mantém glibc → ZGC Generacional
# continua disponível. A tag :nonroot executa como uid 65532 sem configuração extra.
FROM gcr.io/distroless/java21-debian12:nonroot AS runtime

WORKDIR /app

# Camadas em ordem crescente de frequência de mudança:
# dependencies e spring-boot-loader raramente mudam → invalidam menos o cache.
COPY --from=layers --chown=nonroot:nonroot /app/dependencies/          ./
COPY --from=layers --chown=nonroot:nonroot /app/spring-boot-loader/    ./
COPY --from=layers --chown=nonroot:nonroot /app/snapshot-dependencies/ ./
COPY --from=layers --chown=nonroot:nonroot /app/application/           ./

EXPOSE 8080

# Sem shell disponível em distroless — use liveness/readiness probes do orquestrador
# (Kubernetes, Docker Compose healthcheck via curl em sidecar) em vez de HEALTHCHECK aqui.

# ── JVM tuning ────────────────────────────────────────────────────────────────
# Heap   : inicia em 50% e cresce até 75% do memory limit do container.
# GC     : ZGC Generacional (Java 21+) — pausas sub-milissegundo.
#          Requer glibc (Jammy); NÃO disponível em Alpine (musl libc).
# VTs    : virtual threads (spring.threads.virtual.enabled=true).
# OOM    : ExitOnOutOfMemoryError reinicia via orchestrator em vez de degradar.
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
