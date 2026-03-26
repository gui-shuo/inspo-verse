# ============================================
# Inspo-Verse Frontend - 多阶段构建
# ============================================

# -------- Stage 1: Node 构建 --------
FROM node:20-alpine AS builder

WORKDIR /build

# 先复制依赖描述文件利用缓存
COPY package.json ./
RUN npm install

# 复制源码并构建
COPY . .
RUN npm run build

# -------- Stage 2: Nginx 运行时 --------
FROM nginx:1.27-alpine

LABEL maintainer="Inspo-Verse Team"

# 移除默认配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/inspo-verse.conf

# 从构建阶段复制静态资源
COPY --from=builder /build/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
