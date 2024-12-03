
# Use a JDK17 as set in the dev environment
FROM openjdk:17-jdk-alpine 

# If there si the need to upgrade to a newer version of Java use the following FROM
# FROM alpine/java:21-jdk

# Set the workdir in the container
WORKDIR /usr/app

# Copy JAR in the working directory
COPY target/invoicing-service.jar invoicing-service.jar

# Espose port 8080
EXPOSE 8080

# Comand to run the Spring Boot application
ENTRYPOINT ["java","-jar","invoicing-service.jar"]