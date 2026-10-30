# syntax=docker/dockerfile:1

# ---------- Stage 1: build ----------
FROM node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
RUN npm run build -- --configuration=production

# ---------- Stage 2: serve ----------
FROM nginx:1.27-alpine
# Angular "browser" builder outputs directly into dist/learning
COPY --from=builder /app/dist/learning /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template

# BACKEND_URL is substituted into the nginx template at container start
ENV BACKEND_URL=http://backend:8005
EXPOSE 80

