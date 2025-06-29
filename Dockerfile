# Use an official OpenJDK image
FROM eclipse-temurin:24-jdk-alpine

# Set work directory
WORKDIR /app

# Copy the jar file
# COPY target/temjaimusic-0.0.1-SNAPSHOT.jar temjaimusic-0.0.1-SNAPSHOT.jar
COPY . .

# Expose port (use environment variable from Railway)
EXPOSE 8080

# Run the jar
CMD ["java", "-jar", "temjaimusic-0.0.1-SNAPSHOT.jar"]

