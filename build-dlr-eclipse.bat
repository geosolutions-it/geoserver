@echo off
mvn -f src/pom.xml eclipse:clean eclipse:eclipse -Pwps,wps-cluster-hazelcast,wps-remote,importer,security,dyndimension,colormap,netcdf,netcdf-out,rest-ext,jms-cluster -DskipTests