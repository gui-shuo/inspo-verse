# ============================================
# Inspo-Verse Java API - 多阶段构建
# ============================================

# -------- Stage 1: Maven 构建 --------
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 先复制 pom.xml 利用 Docker 缓存下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源码并打包（跳过测试加速构建）
COPY src ./src
RUN mvn clean package -DskipTests -B

# -------- Stage 2: 精简运行时 --------
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Inspo-Verse Team"

# 创建非 root 用户
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# 从构建阶段复制 JAR
COPY --from=builder /build/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /tmp/inspo-uploads && chown -R app:app /tmp/inspo-uploads

# 切换到非 root 用户
USER app

EXPOSE 8080

# JVM 调优参数：容器感知内存、GC 优化
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=65.0", \
  "-XX:+UseSerialGC", \
  "-Xss512k", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
