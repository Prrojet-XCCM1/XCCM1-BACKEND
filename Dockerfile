FROM eclipse-temurin:21
WORKDIR /app

# Copier les fichiers
COPY . .

# Build et run
RUN ./mvnw clean package -DskipTests

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/*.jar"]