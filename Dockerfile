# Stage 1: Build the application using Gradle
FROM gradle:8.10-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# This creates the executable files
RUN gradle installDist --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:17-jre
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/install/ /app/

# Replace 'your-project-name' with the actual name of your project folder inside build/install/
# (This is usually the name of your root project folder)
WORKDIR /app/infiflyrecipeserver/bin
CMD ["./infiflyrecipeserver"]
