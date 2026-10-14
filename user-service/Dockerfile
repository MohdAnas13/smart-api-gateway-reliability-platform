FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN chmod +x mvnw
RUN ./mvnw -DskipTests package
EXPOSE 8081
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "target/user-service-0.0.1-SNAPSHOT.jar"]
