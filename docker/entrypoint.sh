#!/usr/bin/env bash

if [ -z "$APP_LOCATION" ]; then
  echo "WARN: APP_LOCATION not defined or empty!"
fi

webapp_path="${CATALINA_BASE}/webapps/${APP_LOCATION}"

if [ -d "$webapp_path" ]; then
  echo "INFO: Webapp found at desired path"
else
  echo "INFO: Trying to rename webapp to ${APP_LOCATION}"
  mv -v ${CATALINA_BASE}/webapps/geoserver "$webapp_path" || exit 1
fi

cd "${CATALINA_BASE}/bin"
catalina.sh run
