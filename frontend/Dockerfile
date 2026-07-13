# Sintaxe Docker
# syntax=docker/dockerfile:1

# ============================================================
# ERP TopPower — imagem do frontend React + Vite
# Build multi-stage:
#   1) compila os estáticos com Node (vite build -> dist/)
#   2) serve com nginx (imagem final leve)
# ============================================================

# ---------- Stage 1: build ----------
FROM node:20-alpine AS build

WORKDIR /app

# Copia primeiro os manifests para cachear a instalação de deps.
COPY package.json package-lock.json* ./
RUN npm ci

# Copia o resto do código e gera os estáticos em dist/.
COPY . .

# Em produção, o navegador precisa apontar para o backend.
# Passamos a URL pública do backend via var de build do Vite.
ARG VITE_API_URL=/api
ENV VITE_API_URL=${VITE_API_URL}
RUN npm run build

# ---------- Stage 2: runtime (nginx) ----------
FROM nginx:alpine

# Copia os estáticos gerados para o diretório servido pelo nginx.
COPY --from=build /app/dist /usr/share/nginx/html

# Configuração do nginx: SPA fallback + proxy /api -> backend.
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]