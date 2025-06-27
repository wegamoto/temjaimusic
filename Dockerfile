FROM eclipse-temurin:24-jdk-alpine

WORKDIR /app

# ใช้ wildcard เพื่อครอบคลุมชื่อ jar ที่เปลี่ยนได้
COPY target/*.jar temjaimusic.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "temjaimusic.jar"]

