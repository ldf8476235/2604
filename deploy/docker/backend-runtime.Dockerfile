FROM eclipse-temurin:8-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY backend/platform-0.1.0.jar /app/platform.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/platform.jar"]
