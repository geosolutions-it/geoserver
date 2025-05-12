FROM tomcat:9-jdk11-openjdk as mother
LABEL maintainer="Alessandro Parma <alessandro.parma@geosolutionsgroup.com>"
SHELL ["/bin/bash", "-c"]

# download and install libjpeg-2.0.6 from sources.
ARG DEBIAN_FRONTEND=noninteractive
ARG CMAKE_BUILD_PARALLEL_LEVEL=8
ARG APP_LOCATION="geoserver"

# accepts local files and URLs. Tar(s) are automatically extracted
#WORKDIR /output/datadir
#RUN ls -l
#ARG GEOSERVER_DATA_DIR_SRC="./.placeholder"
#ADD "${GEOSERVER_DATA_DIR_SRC}" "./"

# accepts local files and URLs. Tar(s) are automatically extracted
#WORKDIR /output/webapp
#ARG GEOSERVER_WEBAPP_SRC="./.placeholder"
#ADD "${GEOSERVER_WEBAPP_SRC}" "./"

# zip files require explicit extracion
#RUN \
#    if [ "${GEOSERVER_WEBAPP_SRC##*.}" = "zip" ]; then \
#        unzip "./*zip"; \
#        rm ./*zip; \
#    fi \
#    && [ -d "./geoserver" ] || (mkdir -p ./geoserver && unzip ./geoserver.war -d ./geoserver && rm ./geoserver.war)

#WORKDIR /output/plugins
#ARG PLUG_IN_URLS=""
#ARG PLUG_IN_PATHS=""
#ADD .placeholder ${PLUG_IN_PATHS} /output/plugins/
#COPY geoserver-plugin-download.sh /usr/local/bin/geoserver-plugin-download.sh
#RUN /usr/local/bin/geoserver-plugin-download.sh /output/plugins/ ${PLUG_IN_URLS}
#RUN \
#    if [ -f *.zip ] ; then \
#       unzip -o "./*.zip"; \
#    fi

#WORKDIR /output/webapp
#RUN \
#    if [ "${APP_LOCATION}" != "geoserver" ]; then \
#      mv /output/webapp/geoserver /output/webapp/${APP_LOCATION}; \
#    fi

FROM tomcat:9-jdk11-openjdk

ARG UID=99
ARG GID=99
ARG UNAME=tomcat
ARG CUSTOM_FONTS="./.placeholder"
ENV ADMIN_PASSWORD=""
ENV APP_LOCATION="geoserver"

ENV CATALINA_BASE "$CATALINA_HOME"

# Enforce usage of distribution own Java (https://osgeo-org.atlassian.net/browse/GEOS-11417)
ENV PATH="/usr/lib/jvm/java-11-openjdk-amd64/bin:$PATH"
ENV JAVA_HOME="/usr/lib/jvm/java-11-openjdk-amd64"

# set externalizations
ENV GEOSERVER_HOME="/var/geoserver"
ENV GEOSERVER_LOG_DIR="${GEOSERVER_HOME}/logs"
ENV GEOSERVER_DATA_DIR="${GEOSERVER_HOME}/datadir"
ENV GEOSERVER_LOG_LOCATION="${GEOSERVER_LOG_DIR}/${DOCKER_HOST}-${MASTER_SLAVE}-${HOSTNAME}.log"
ENV GEOWEBCACHE_CONFIG_DIR="${GEOSERVER_HOME}/gwc_cache_dir"
ENV GEOSERVER_AUDIT_PATH="${GEOSERVER_HOME}/audits/geoserver-${DOCKER_HOST}-${MASTER_SLAVE}-${HOSTNAME}"
ENV GEOWEBCACHE_CACHE_DIR="${GEOSERVER_HOME}/gwc_cache_dir"
ENV FONTS_DIR=${JAVA_HOME}/lib/fonts
# override at run time as needed JAVA_OPTS
ENV INITIAL_MEMORY="2G"
ENV MAXIMUM_MEMORY="4G"
ENV JAIEXT_ENABLED="true"
ENV PLUGIN_DYNAMIC_URLS=""
ENV OOM_DUMP_DIR="${GEOSERVER_HOME}/memory_dumps"
ENV GEOSERVER_OPTS=" \
  -Dorg.geotools.coverage.jaiext.enabled=${JAIEXT_ENABLED} \
  -Duser.timezone=UTC \
  -Dorg.geotools.shapefile.datetime=true \
  -DGEOSERVER_LOG_LOCATION=${GEOSERVER_LOG_LOCATION} \
  -DGEOWEBCACHE_CONFIG_DIR=${GEOWEBCACHE_CONFIG_DIR} \
  -DGEOWEBCACHE_CACHE_DIR=${GEOWEBCACHE_CACHE_DIR} \
  -DGEOSERVER_AUDIT_PATH=${GEOSERVER_AUDIT_PATH} \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=${OOM_DUMP_DIR}/geoserver-${DOCKER_HOST}-${MASTER_SLAVE}.dump \
  -Dorg.geoserver.wms.featureinfo.minBuffer=10 \
  -Dorg.geotools.coverage.io.netcdf.enhance.ScaleMissing=true \
  -DDOWNLOAD_MAP_TRANSPARENT=true \
  -DENTITY_RESOLUTION_ALLOWLIST=www.w3.org|schemas.opengis.net|www.opengis.net|inspire.ec.europa.eu/schemas"

ENV JAVA_OPTS="-Xms${INITIAL_MEMORY} -Xmx${MAXIMUM_MEMORY} \
  -Djava.awt.headless=true -server \
  -Dfile.encoding=UTF8 \
  -Djavax.servlet.request.encoding=UTF-8 \
  -Djavax.servlet.response.encoding=UTF-8 \
  -XX:SoftRefLRUPolicyMSPerMB=36000 -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 -XX:ParallelGCThreads=20 -XX:ConcGCThreads=5 \
  ${GEOSERVER_OPTS}"

ENV LANG=C.UTF-8
# added for git hash
ARG GIT_HASH=""
ENV GIT_HASH "$GIT_HASH"

#COPY run_tests.sh /docker/tests/run_tests.sh

# install needed packages and create externalized dirs
ARG DEBIAN_FRONTEND=noninteractive
RUN mkdir -p \
    "${GEOSERVER_DATA_DIR}" \
    "${GEOSERVER_LOG_DIR}"  \
    "${GEOWEBCACHE_CONFIG_DIR}" \
    "${GEOWEBCACHE_CACHE_DIR}"

RUN apt-get update
RUN apt-get install --yes git vim gdal-bin postgresql-client fontconfig libfreetype6 jq unzip apache2-utils openjdk-11-jdk-headless
RUN apt-get clean \
    && apt-get -y autoclean \
    && apt-get -y autoremove \
    && rm -rf /var/lib/apt/lists/* \
    && rm -rf /usr/share/man/* \
    && rm -rf /usr/share/doc/*

# copy from mother
#COPY --from=mother "/output/datadir" "${GEOSERVER_DATA_DIR}"
#COPY --from=mother "/output/webapp/geoserver" "${CATALINA_BASE}/webapps/geoserver"
#COPY --from=mother "/output/plugins" "${CATALINA_BASE}/webapps/geoserver/WEB-INF/lib"
COPY "./resources/geoserver-datadir" "${GEOSERVER_DATA_DIR}"
RUN mkdir "$CATALINA_BASE/webapps/geoserver"
WORKDIR "$CATALINA_BASE/webapps/geoserver"
COPY "./resources/geoserver/geoserver.war" "./"
RUN unzip ./geoserver.war 
RUN rm -f geoserver.war
#RUN \
#    if [ "${APP_LOCATION}" != "geoserver" ]; then \
#      mv ${CATALINA_BASE}/webapps/geoserver.war ${CATALINA_BASE}/webapps/${APP_LOCATION}.war; \
#    fi
RUN echo `ls -l "${CATALINA_BASE}/webapps"`

#COPY geoserver-plugin-download.sh /usr/local/bin/geoserver-plugin-download.sh
RUN mkdir ${JAVA_HOME}/lib/fonts
#ADD ${CUSTOM_FONTS} ${FONTS_DIR}

COPY geoserver-rest-config.sh /usr/local/bin/geoserver-rest-config.sh
COPY geoserver-rest-reload.sh /usr/local/bin/geoserver-rest-reload.sh

RUN groupadd -g $GID $UNAME
RUN useradd -m -u $UID -g $GID --system $UNAME
RUN chown -R $UID:$GID $GEOSERVER_LOG_DIR $CATALINA_BASE $GEOWEBCACHE_CACHE_DIR $GEOWEBCACHE_CONFIG_DIR $GEOSERVER_DATA_DIR

WORKDIR "$CATALINA_BASE"
COPY entrypoint.sh ${CATALINA_BASE}/entrypoint.sh
RUN chown $UID:$GID ${CATALINA_BASE}/entrypoint.sh && chmod +x ${CATALINA_BASE}/entrypoint.sh

USER $UNAME

ENV TERM xterm
EXPOSE 8080/tcp
CMD ["/usr/local/tomcat/entrypoint.sh"]
