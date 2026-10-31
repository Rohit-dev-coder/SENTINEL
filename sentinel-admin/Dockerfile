# Build from the repository root:  docker build -f sentinel-admin/Dockerfile .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -q -pl sentinel-admin -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system --gid 1001 sentinel \
    && useradd --system --uid 1001 --gid sentinel --no-create-home sentinel
COPY --from=build /build/sentinel-admin/target/sentinel-admin-1.0.0.jar app.jar
USER sentinel
EXPOSE 8081 9081
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
