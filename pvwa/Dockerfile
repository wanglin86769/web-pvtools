# Use an official Maven image as the base image
FROM maven:3.9.7-sapmachine-21 AS build
# Set the working directory in the container
WORKDIR /app
# Copy the pom.xml and the project files to the container
COPY pom.xml .
COPY src ./src
# Build the application using Maven
RUN mvn clean package -DskipTests

# Use an official OpenJDK image as the base image
FROM openjdk:21-jdk
# Set environment variables
ENV EPICS_CA_ADDR_LIST "10.1.236.131"
ENV EPICS_PVA_ADDR_LIST "10.1.236.131"
# Set the working directory in the container
WORKDIR /app
# Copy the built JAR file from the previous stage to the container
COPY --from=build /app/target/pvwa-0.0.1-SNAPSHOT.jar .
# Set the command to run the application
CMD ["java", "-jar", "pvwa-0.0.1-SNAPSHOT.jar"]