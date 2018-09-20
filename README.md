GeoWeb-BackEnd
=====

[![Build Status](https://api.travis-ci.org/KNMI/GeoWeb-BackEnd.svg?branch=master)](https://travis-ci.org/KNMI/GeoWeb-BackEnd)

Back end for GeoWeb

For setting up development environment:

1) Download and install spring tool suite (https://spring.io/tools/sts/all)
2) Start STS and import this project as existing project
3) Press alt F5 to update Maven
4) In STS, select Run as java application
5) Select GeoWebBackEndApplication
6) To adjust the server port, set in Run Configuration the argument like this: --server.port=8090
7) Copy pre-commit to ./git/hooks to enable automatic unit testing on new commits.

For creating a new package:

1) Adjust the version in pom.xml: 0.<sprint number>.version (this is named ${VERSION} from now on)
2) Type mvn package
3) in directory target the file ./target/demo-${VERSION}-SNAPSHOT.jar is created.
4) You can for example start this with java -jar demo-${VERSION}-SNAPSHOT.jar

Creating a docker image:

1) mvn install dockerfile:build



