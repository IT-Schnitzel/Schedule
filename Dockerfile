# Этап 1: сборка приложения с использованием Maven и JDK 21
FROM maven:3.8.6-openjdk-21 as build

WORKDIR /app
COPY . /app
RUN mvn clean package -DskipTests

# Этап 2: запуск приложения с использованием JDK 21
FROM openjdk:21

# Копируем собранный JAR-файл из этапа сборки
COPY --from=build /app/target/schedule-management-service-1.0-SNAPSHOT.jar app.jar

# Запуск приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
