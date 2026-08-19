# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build
WORKDIR /app

ARG NPM_REGISTRY=https://registry.npmmirror.com
COPY frontend/package.json frontend/pnpm-lock.yaml* ./
RUN --mount=type=cache,target=/pnpm/store \
    corepack enable \
    && pnpm config set registry ${NPM_REGISTRY} \
    && pnpm config set store-dir /pnpm/store \
    && pnpm install --frozen-lockfile

COPY frontend/ .
RUN pnpm build

FROM node:22-alpine
WORKDIR /app

ENV NODE_ENV=production
ENV HOST=0.0.0.0
ENV PORT=5173
ENV VITE_PROXY_TARGET=http://172.17.0.1:8001

COPY --from=build /app/package.json /app/pnpm-lock.yaml ./
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=build /app/index.html ./index.html
COPY --from=build /app/vite.config.ts ./vite.config.ts

EXPOSE 5173

CMD ["sh", "-c", "node_modules/.bin/vite preview --host ${HOST} --port ${PORT}"]
