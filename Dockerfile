# =============== Build Stage =====================
FROM eclipse-temurin:24-jdk-alpine AS build

WORKDIR /workspace/app

# Copy Maven wrapper & pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Package the JAR
RUN ./mvnw clean package -DskipTests

# =============== Run Stage =====================
# Use an official OpenJDK image
FROM eclipse-temurin:24-jdk-alpine

WORKDIR /app

# Copy JAR from build stage
COPY --from=build /workspace/app/target/*.jar app.jar

# Expose port (use environment variable from Railway)
EXPOSE 8080

# Run the jar
CMD ["java", "-jar", "app.jar"]

