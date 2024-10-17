# Use an official Java runtime as a parent image
FROM openjdk:11-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the project JAR file into the container
COPY target/trade-microservice-0.0.1-SNAPSHOT.jar /app/trade-microservice.jar

# Run the JAR file
ENTRYPOINT ["java", "-jar", "/app/trade-microservice.jar"]
