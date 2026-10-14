FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN chmod +x mvnw
RUN ./mvnw -DskipTests package
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-Dspring.profiles.active=docker", "-jar", "target/api-gateaway-0.0.1-SNAPSHOT.jar"]
