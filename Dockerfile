# Use Eclipse Temurin OpenJDK 17 as base image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mkoba-system/mvnw mkoba-system/.mvn mkoba-system/pom.xml ./
COPY mkoba-system/.mvn .mvn

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY mkoba-system/src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose port 8080
EXPOSE 8080

# Run the application
CMD ["java", "-jar", "target/mkoba-system-0.0.1-SNAPSHOT.jar"]