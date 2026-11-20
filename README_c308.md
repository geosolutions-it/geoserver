# GeoServer 2.28.6_c308 (Radarmeteo)

Official GeoServer 2.28.6 built against the official GeoTools 34.6, with the NetCDF-Java libraries upgraded from 5.5.3 to **5.6.0**.
In particular, 5.6.0 contains the fix for the [netcdf-java issue 1331](https://github.com/Unidata/netcdf-java/issues/1331) concurrency problem, which was the only reason for the previous custom GeoTools/GeoServer `_c308` builds. No custom GeoTools build is needed anymore.

Changes compared to the `2.28.6` tag:

- Version `2.28.6_c308` in all the poms.
- `src/pom.xml`: the `netcdf.version` property (5.6.0) overrides the NetCDF artifacts (`cdm-core`, `netcdf4`, `grib`, `bufr`, `httpservices`, `udunits`) managed by the GeoTools BOM.

## Build the WAR

The WAR includes the WPS, NetCDF, NetCDF output, Importer, WMTS multidimensional, ncWMS and PNG wind plugins. **GRIB is not included**.
From the `src` folder:

```
mvn clean install -DskipTests -Pwps,netcdf,netcdf-out,importer,wmts-multi-dimensional,ncwms,png-wind
```

The WAR is then available at `src/web/app/target/geoserver.war`.

## Add the netcdf-timeseries jar

Build [netcdf-timeseries](https://github.com/geosolutions-it/netcdf-timeseries) (branch area_stats_28x) after the step above, since it depends on the `2.28.6_c308` artifacts installed in the local Maven repository (its pom must have `gs.version` set to `2.28.6_c308`):

```
cd netcdf-timeseries
mvn clean install -DskipTests
```

## Assemble and check the final WAR

From the GeoServer root folder:

```
./build_c308_war.sh <netcdf-timeseries dir> [output dir]
```

The script copies `src/web/app/target/geoserver.war` to `geoserver-2.28.6_c308.war` in the output folder (default `target-c308`), adds the netcdf-timeseries jar to `WEB-INF/lib`, and writes the jar list to `geoserver-2.28.6_c308-libs.txt`.
It then checks that:

- all the plugin jars and the netcdf-timeseries jar are there, and no GRIB jar is;
- all the NetCDF jars are 5.6.0;
- all the GeoTools jars are 34.6 and all the GeoServer jars are 2.28.6_c308;
- no library is there twice in different versions.

It exits with a non-zero status if any check fails.
