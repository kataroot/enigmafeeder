FROM docker.io/library/openjdk:11-jdk-slim as build
LABEL maintainer="Oscar Ortega <oscar@fakemail.com>"

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
 
COPY src ./src
RUN ./mvnw clean package

FROM docker.io/library/openjdk:11-jdk-slim as run

ARG VERSION
ENV VERSION ${VERSION}

RUN adduser --no-create-home --disabled-password --gecos -q --uid 1111 --shell /bin/sh twsuser

WORKDIR /app

COPY --from=build /app/target/enigmafeeder-${VERSION}.jar ./

EXPOSE 8080

USER twsuser
CMD java -jar enigmafeeder-${VERSION}.jar