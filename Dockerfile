FROM eclipse-temurin:21-jdk

WORKDIR /app

# Quick heads up: Check if SNAPSHOP is a typo for SNAPSHOT in your build artifact!
COPY target/class3-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms512m", "-Xmx1536m", "-jar", "app.jar"]