# Build from the repository root:  docker build -f downstream-services/sample-service-2/Dockerfile .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -q -pl downstream-services/sample-service-2 -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system --gid 1001 sentinel \
    && useradd --system --uid 1001 --gid sentinel --no-create-home sentinel
COPY --from=build /build/downstream-services/sample-service-2/target/sample-service-2-1.0.0.jar app.jar
USER sentinel
EXPOSE 8083
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
