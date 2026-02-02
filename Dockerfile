
# Use a JDK17 as set in the dev environment
FROM eclipse-temurin:17-jdk-jammy

# Install curl
RUN apt-get update && apt-get install -y curl \
    && rm -rf /var/lib/apt/lists/*

# Set the workdir in the container
WORKDIR /usr/app

# Copy JAR in the working directory
COPY target/invoicing-service.jar invoicing-service.jar

# Espose port 8080
EXPOSE 8080

# Comand to run the Spring Boot application
ENTRYPOINT ["java","-jar","invoicing-service.jar"]