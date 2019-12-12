GeoWeb-BackEnd
======

[![Build Status](https://api.travis-ci.org/KNMI/GeoWeb-BackEnd.svg?branch=master)](https://travis-ci.org/KNMI/GeoWeb-BackEnd)

Back end for GeoWeb

For setting up development environment with STS:

0) Make sure you have JDK 8 (1.8) installed, the project currently doesn't work with version 9 (higher versions we did not test yet)
1) Download and install spring tool suite (https://spring.io/tools/sts/all)
2) Download lombok.jar (https://projectlombok.org/download.html)
3) Install lombok into spring tool suite with java -jar lombok.jar
3) Start STS and import this project as existing project
4) Press alt F5 to update Maven
5) In STS, select Run as java application
6) Select GeoWebBackEndApplication
7) To adjust the server port, set in Run Configuration the argument like this: --server.port=8090
8) Copy pre-commit to ./git/hooks to enable automatic unit testing on new commits.

For creating a new package:

1) Adjust the version in pom.xml: 0.<sprint number>.version (this is named ${VERSION} from now on)
2) Type mvn package
3) in directory target the file ./target/demo-${VERSION}-SNAPSHOT.jar is created.
4) You can for example start this with java -jar demo-${VERSION}-SNAPSHOT.jar

Creating a docker image:

1) mvn install dockerfile:build

# Setting up environment for Visual Studio Code

Install the following extensions in visual studio code:
* Lombok
* Spring Boot Extension Pack
* Java Extension Pack
* Spring boot support

Install maven dependencies:
* mvn clean
* mvn package

Set the profile to generic

* In geoweb-backend/src/main/resources/application.yml Change `spring->profiles->active` to `generic`

Start with spring boot dashboard

Visit http://localhost:8080/versioninfo/version


# OAuth2 with Amazon Cognito

Setup Cognito on AWS

* Create a user pool
* Set required attributes (at least email)
* Add users in 'General settings'->'Users and groups'
* Add an application client in 'General settings'->'App clients' Here you get the user pool, client id and client secret. These need to be configured in the geoweb backend, either via commandline settings (below) or launch configurations.
* In 'App integration'->'App client settings' Make sure that the 'Cognito User Pool' checkbox is clicked. 
* As callback urls configure:  `https://<geowebbackendurl>/login`
* As Sign out url configure: `https://<geowebbackendurl>/logout/geoweb`


In order to run the backend with AWS cognito, the backend needs to run over https. On development machines this can be achieved by generating a self signed certificate and enabling SSL for tomcat.

A self signed certificate can be generated via:
```
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 -keystore ~/keystore.jks -validity 3650
```

To start the GeoWeb backend from commandline:

```
mvn spring-boot:run -Dspring-boot.run.arguments=\
--security.oauth2.client.clientSecret=***,\
--security.oauth2.client.clientId=7kjte51escl78atbuqe8348v98,\
--client.userpool=gw-sesar-test-appclient,\
--spring.profiles.active=oauth2-cognito,\
--server.ssl.enabled=true,\
--server.port=8443,\
--client.frontendURL=http://localhost:3000/,\
--client.backendURL=https://localhost:8443/
```


To start from visual studio code, you have to edit your launch configuration (launch.json):

```
   {
      "type": "java",
      "name": "Spring Boot-GeoWebBackEndApplication<geoweb-backend>",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "console": "internalConsole",
      "mainClass": "nl.knmi.geoweb.backend.GeoWebBackEndApplication",
      "projectName": "geoweb-backend",
      "args": [
            "--server.ssl.enabled=true",
            "--server.port=8443",
            "--security.oauth2.client.clientId=7kjte51escl78atbuqe8348v98"
            "--security.oauth2.client.clientSecret=***",
            "--client.userpool=gw-sesar-test-appclient",
            "--spring.profiles.active=oauth2-cognito",
            "--client.frontendURL=http://localhost:3000/",
            "--client.backendURL=https://localhost:8443",
        ]
    }
```


# Logging backend information

## Logging system

    Simple Logging Facade for Java (SLF4J)

## Logging levels

| Env  | Name | Example |
|---|---|---|
| P  |ERROR   | "IWXXM invalid" |
| P  |WARN   | "TAF export failed" "Only first geometry was used" |
| P  |INFO   | "TAF is made" |
| D  |DEBUG   | "There are 6 products"  |
| D  |TRACE  | "export is called" "export returned"  |

P = Production; D = Development

## Hierarchy

Logging will be possible by module.

## Stacktraces

Printing of stacktraces has been replaced by logging the related error message.
