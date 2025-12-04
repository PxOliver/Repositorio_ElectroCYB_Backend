# Etapa 1: Compilar con Maven
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copiar todo el proyecto al contenedor
COPY . .

# ✅ Dar permisos de ejecución al wrapper de Maven
RUN chmod +x mvnw

# Empaquetar el JAR (sin tests)
RUN ./mvnw -DskipTests package

# Etapa 2: Imagen final
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiar JAR generado desde la etapa de build
COPY --from=build /app/target/electrocyb_backend-0.0.1-SNAPSHOT.jar app.jar

# Render asigna dinámicamente el puerto en la variable PORT
ENV PORT=8080

EXPOSE 8080

# Comando de ejecución
CMD ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]