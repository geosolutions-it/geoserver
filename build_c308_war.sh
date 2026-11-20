#!/usr/bin/env bash
# Assembles the Radarmeteo GeoServer WAR (base WAR + netcdf-timeseries jar), then lists and checks its libraries.
# Usage: build_c308_war.sh <netcdf-timeseries dir> [output dir]
set -euo pipefail

GS_DIR=$(cd "$(dirname "$0")" && pwd)
TS_DIR=${1:?usage: $0 <netcdf-timeseries dir> [output dir]}
OUT_DIR=${2:-$GS_DIR/target-c308}
NETCDF_VERSION=5.6.0
GT_VERSION=34.6
REQUIRED=(gs-wps-core gs-web-wps gs-netcdf gs-netcdf-out gs-importer-core gs-importer-web gs-importer-rest
  gs-wmts-multi-dimensional gs-ncwms gs-png-wind netcdf-timeseries)
FORBIDDEN=(gs-grib gt-grib grib)

BASE_WAR=$GS_DIR/src/web/app/target/geoserver.war
TS_JAR=$(ls "$TS_DIR"/target/netcdf-timeseries-*.jar | grep -v -e sources -e javadoc)
[[ -f $BASE_WAR ]] || { echo "Base WAR not found: $BASE_WAR"; exit 1; }
GS_VERSION=$(unzip -Z1 "$BASE_WAR" | sed -n 's#^WEB-INF/lib/gs-main-\(.*\)\.jar$#\1#p')
WAR=$OUT_DIR/geoserver-$GS_VERSION.war

# assemble
STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT
mkdir -p "$OUT_DIR" "$STAGE/WEB-INF/lib"
cp "$BASE_WAR" "$WAR"
cp "$TS_JAR" "$STAGE/WEB-INF/lib/"
(cd "$STAGE" && jar uf "$WAR" WEB-INF/lib/*.jar)

# list
LIBS=$OUT_DIR/geoserver-$GS_VERSION-libs.txt
unzip -Z1 "$WAR" | sed -n 's#^WEB-INF/lib/\(.*\.jar\)$#\1#p' | sort > "$LIBS"
echo "WAR: $WAR ($(du -h "$WAR" | cut -f1), $(wc -l < "$LIBS") jars, list in $LIBS)"
grep -E "^(cdm-core|netcdf4|bufr|httpservices|udunits)-[0-9]|^netcdf-timeseries" "$LIBS"

# check
ERRORS=0
fail() { echo "FAIL: $*"; ERRORS=$((ERRORS + 1)); }
for a in "${REQUIRED[@]}"; do
  grep -qE "^$a-[0-9]" "$LIBS" || fail "missing $a"
done
for a in "${FORBIDDEN[@]}"; do
  if grep -qE "^$a-[0-9]" "$LIBS"; then fail "unexpected $a"; fi
done
for a in cdm-core netcdf4 bufr httpservices udunits; do
  grep -qx "$a-$NETCDF_VERSION.jar" "$LIBS" || fail "$a is not $NETCDF_VERSION: $(grep -E "^$a-[0-9]" "$LIBS" || echo missing)"
done
for j in $(grep -E "^gt-" "$LIBS" | grep -v -- "-$GT_VERSION.jar"); do fail "unexpected GeoTools version: $j"; done
for j in $(grep -E "^gs-" "$LIBS" | grep -v -- "-$GS_VERSION.jar"); do fail "unexpected GeoServer version: $j"; done
# the same artifact twice, in different versions, makes the class loading order undefined
for a in $(sed -E 's/-[0-9].*\.jar$//' "$LIBS" | sort | uniq -d); do
  fail "duplicate artifact: $(grep -E "^$a-[0-9]" "$LIBS" | tr '\n' ' ')"
done

[[ $ERRORS -eq 0 ]] && echo "OK: all checks passed" || { echo "$ERRORS check(s) failed"; exit 1; }
