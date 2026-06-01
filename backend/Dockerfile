FROM maven:3.9.9-eclipse-temurin-8 AS build
WORKDIR /workspace
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
RUN mvn -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:8-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=build /workspace/backend/target/platform-0.1.0.jar /app/platform.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/platform.jar"]
