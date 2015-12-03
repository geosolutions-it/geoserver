@echo off
rem mvn -f src/pom.xml eclipse:clean eclipse:eclipse clean install -Pwps,wps-cluster-hazelcast,wps-remote,importer,security,rest-ext -DskipTests -U
mvn -f src/pom.xml eclipse:clean eclipse:eclipse clean install -Pwps,wps-remote,wps-jdbc,importer,security,rest-ext -DskipTests -U