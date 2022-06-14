FROM openjdk:17.0.2-jdk-slim-buster
RUN apt upgrade && addgroup megamarket --disabled-password && \
    adduser megamarket --ingroup megamarket && \
    mkdir -p /app && \
    chown -R megamarket:megamarket /app
WORKDIR /app
USER megamarket:megamarket
COPY target/megamarket.jar app.jar
ENV DB_HOST=DB_HOST \
    DB_PORT=DB_PORT \
    DB_NAME=DB_NAME \
    DB_USER=DB_USER \
    DB_PASS=DB_PASS
ENTRYPOINT java -DDB_HOST=$DB_HOST \
                -DDB_PORT=$DB_PORT \
                -DDB_NAME=$DB_NAME \
                -DDB_USER=$DB_USER \
                -DDB_PASS=$DB_PASS \
                -jar app.jar