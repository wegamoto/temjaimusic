FROM eclipse-temurin:24-jdk-alpine

# สร้าง directory สำหรับ app
WORKDIR /app

# คัดลอก JAR เข้า container
COPY target/temjaimusic-0.0.1-SNAPSHOT.jar app.jar

# เปิดพอร์ต (ถ้าใช้ Railway จะ expose อัตโนมัติ แต่ใส่ไว้ก็ดี)
EXPOSE 8080

# สั่งให้รัน
ENTRYPOINT ["java", "-jar", "app.jar"]
