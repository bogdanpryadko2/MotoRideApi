FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 motoride && useradd --uid 10001 --gid motoride --no-create-home motoride
WORKDIR /app
COPY --from=build --chown=motoride:motoride /app/target/*.jar app.jar
USER motoride
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 CMD curl --fail --silent http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
