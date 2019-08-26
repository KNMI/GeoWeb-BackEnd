FROM maven:3-jdk-8-alpine
COPY geoweb-backend.jar /geoweb-backend.jar
RUN mkdir -p /tmp/admin/locations/
COPY docker/locations.dat /tmp/admin/locations/locations.dat
ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /geoweb-backend.jar" ]

#docker build -t geoweb-backend .
#docker run -p 8080:8080 -it geoweb-backend
