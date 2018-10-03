FROM maven:3-jdk-8-alpine
VOLUME /tmp
RUN mkdir /src
WORKDIR /src
COPY . geoweb-backend
WORKDIR /src/geoweb-backend/
RUN mvn package
RUN cp ./target/geoweb-backend-*.jar /src/geoweb-backend.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /src/geoweb-backend.jar" ]

