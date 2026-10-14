FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app
COPY --from=build /workspace/target/skillgap-ai-backend-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
EXPOSE 10000
ENV PORT=10000
ENTRYPOINT ["sh", "-c", "exec java -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar /app/app.jar"]
