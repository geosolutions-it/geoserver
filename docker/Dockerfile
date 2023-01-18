ARG BASE_IMAGE_NAME=gs-base
ARG BASE_IMAGE_TAG=7.0-jre8
FROM geosolutionsit/${BASE_IMAGE_NAME}:${BASE_IMAGE_TAG}
LABEL maintainer=alessandro.parma@geo-solutions.it

RUN  export DEBIAN_FRONTEND=noninteractive
ENV  DEBIAN_FRONTEND noninteractive
RUN  dpkg-divert --local --rename --add /sbin/initctl
#RUN  ln -s /bin/true /sbin/initctl

ENV jpeg_turbo_version 1.5.3

# Install updates
# Install Utils 
# Install GDAL
# Install JPEG Turbo 
# Cleanup 
RUN apt-get -y update \
 && apt-get install -y vim zip unzip net-tools telnet procps postgresql-client-9.6 gdal-bin \
 && wget --no-check-certificate https://downloads.sourceforge.net/project/libjpeg-turbo/${jpeg_turbo_version}/libjpeg-turbo-official_${jpeg_turbo_version}_amd64.deb \
 && dpkg -i ./libjpeg*.deb \
 && apt-get -f install \
 && rm -rf /tmp/resources \
 && rm -rf /var/lib/apt/lists/*


WORKDIR $CATALINA_HOME

# Set ErrorReportValve to hide Tomcat version and exceptions
RUN sed -i \
's/<\/Host/<Valve className="org.apache.catalina.valves.ErrorReportValve"\n\t showReport="false"\n\t showServerInfo="false" \/>\n\t<\/Host/' \
${CATALINA_BASE}/conf/server.xml

ENV TERM xterm
ENV LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/opt/libjpeg-turbo/lib64
# set id/gid default is 99
ARG ID=99
ARG GID=99
# adjust permissions to custom id/gid
RUN chown -R ${ID}:${GID} ${CATALINA_BASE} ${GEOSERVER_HOME} ${GEOWEBCACHE_CACHE_DIR} ${GEOWEBCACHE_CONFIG_DIR} ${OOM_DUMP_DIR} ${GEOSERVER_AUDIT_PATH}
USER $ID:$GID

EXPOSE 8080
