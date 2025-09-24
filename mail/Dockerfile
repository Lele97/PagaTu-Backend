FROM eclipse-temurin:17-jre

WORKDIR /app

# Copia il JAR
COPY target/*.jar app.jar

# Crea esplicitamente una directory per i template (opzionale)
RUN mkdir -p /app/templates

# Copia esplicitamente i template (se necessario)
COPY src/main/resources/templates /app/templates

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]