# Backend obrazu dla prywatnej karty pacjenta.
# Kontekst budowania: katalog backend/ (patrz docker-compose.yml).
#
# Etap 1: buduje wykonywalny JAR Spring Boota (Gradle 8.14.3 + JDK 17,
#         zgodnie z backend/gradle/wrapper/gradle-wrapper.properties).
# Etap 2: uruchamia JAR na lekkim JRE 17 (Alpine), jako użytkownik bez roota.

FROM gradle:8.14.3-jdk17 AS build
WORKDIR /home/gradle/project

COPY --chown=gradle:gradle settings.gradle.kts build.gradle.kts ./
COPY --chown=gradle:gradle gradle ./gradle
COPY --chown=gradle:gradle src ./src

RUN gradle --no-daemon --console=plain bootJar -x test \
    && find build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -exec cp {} app.jar \;

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S app && adduser -S app -G app \
    && mkdir -p /data/storage && chown -R app:app /data

WORKDIR /app
COPY --from=build /home/gradle/project/app.jar /app/app.jar

USER app
ENV STORAGE_ROOT=/data/storage
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
