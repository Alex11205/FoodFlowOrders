FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY event-contracts/pom.xml event-contracts/pom.xml
COPY foodflow/pom.xml foodflow/pom.xml
COPY foodfloworders/pom.xml foodfloworders/pom.xml
COPY event-contracts/src event-contracts/src
COPY foodfloworders/src foodfloworders/src
RUN mvn -q -Dmaven.test.skip=true -pl foodfloworders -am package

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /workspace/foodfloworders/target/*.jar app.jar
COPY foodfloworders/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
