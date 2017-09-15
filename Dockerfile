FROM openjdk:8-jdk-alpine
VOLUME /tmp
#ADD target/demo-0.12.2-SNAPSHOT.jar app.jarADD https://jitpack.io/com/github/KNMI/GeoWeb-BackEnd/sprint_12.2/GeoWeb-BackEnd-sprint_12.2.jar app.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar" ]