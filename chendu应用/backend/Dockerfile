# 两段式构建，但编译在宿主机完成：
#   1) cd backend && mvn clean package        -> target/ai-scaffold-backend.jar
#   2) docker build -t notebook-backend:local ./backend
# 镜像内不再执行 Maven，构建更快，也不需要 Docker 构建阶段拉取 Maven 依赖。
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --chown=spring:spring target/ai-scaffold-backend.jar app.jar

USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
