cd src
#mvn clean install -Pwps,wps-cluster-hazelcast,wps-remote,wps-resource,importer,security,dyndimension,colormap,netcdf,netcdf-out,rest-ext,jms-cluster -DskipTests -U

mvn install -Pguid-filter,wps,wps-cluster-hazelcast,wps-remote,wps-resource,importer,security,dyndimension,colormap,netcdf,netcdf-out,rest-ext,jms-cluster -DskipTests -U

