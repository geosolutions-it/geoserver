cd src

mvn eclipse:clean eclipse:eclipse clean install -Pguid-filter,wps,wps-cluster-hazelcast,wps-remote,wps-resource,wps-mapstoreconfig,importer,security,dyndimension,colormap,netcdf,netcdf-out,rest-ext,jms-cluster -DskipTests