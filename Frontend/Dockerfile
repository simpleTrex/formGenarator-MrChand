# Stage 1: Build Angular App
FROM node:20-alpine AS build
WORKDIR /app

# Copy package and install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build
COPY . .
RUN npm run build -- --configuration=production

# Stage 2: Serve via NGINX
FROM nginx:alpine

# Remove default nginx website
RUN rm -rf /usr/share/nginx/html/*

# Copy built angular assets
COPY --from=build /app/dist/learning /usr/share/nginx/html

# Replace default nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
