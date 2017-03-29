# GeoWeb-BackEnd
Back end for GeoWeb

For setting up development environment:

1) Download and install spring tool suite (https://spring.io/tools/sts/all)
2) Download lombok.jar (https://projectlombok.org/download.html)
3) Install lombok into spring tool suite with java -jar lombok.jar
3) Start STS and import this project as existing project
4) Press alt F5 to update Maven
5) In STS, select Run as java application
6) Select GeoWebBackEndApplication
7) To adjust the server port, set in Run Configuration the argument like this: --server.port=8090

For creating a new package:

1) Adjust the version in pom.xml: 0.<sprint number>.version (this is named ${VERSION} from now on)
2) Type mvn package
3) in directory target the file ./target/demo-${VERSION}-SNAPSHOT.jar is created.
4) You can for example start this with java -jar demo-${VERSION}-SNAPSHOT.jar



