# Étape 1 : Build avec Maven + JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copier uniquement les fichiers nécessaires pour optimiser le cache
COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2 : Image finale uniquement avec le JDK
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copier seulement le JAR final
COPY --from=build /app/target/*.jar app.jar

# Config
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

# Lancer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
