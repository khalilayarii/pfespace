# ---------- ÉTAPE 1 : BUILD ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# On copie d'abord le pom.xml seul pour profiter du cache Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Puis on copie le reste du code source
COPY src ./src

# On build le jar sans relancer les tests
RUN mvn clean package -DskipTests -B

# ---------- ÉTAPE 2 : RUN ----------
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/target/Pfeespace-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]