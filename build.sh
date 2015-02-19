cd src
mvn install -Pwps,wps-cluster-hazelcast,wps-remote,wps-resource,importer,security,dyndimension,colormap,netcdf,netcdf-out,rest-ext,jms-cluster -DskipTests
