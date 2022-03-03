FROM p7hb/docker-spark:2.1.0

MAINTAINER Durga Haridoss

USER root

RUN yum -y install dos2unix net-tools tar rsync

RUN mkdir -p /app/ccr_core && mkdir -p /app/ccr_core/log && mkdir -p /app/keytab &&\
    chmod -R 777 /app/keytab && chmod  -R 777 /app/ccr_core/log

# copy Openshift scripts and docker-entrypoint and .tar.gz  file created by build step
COPY openshift/scripts/*.sh /usr/local/bin
COPY docker-entrypoint.sh /usr/local/bin
COPY target/*tar.gz /app/analyzer

#copy unzipped file into required directory
RUN cd /app/analyzer && tar -xzf *.tar.gz && rm *.tar.gz && \
    ln -sf /app/analyzer/analyzer* /app/analyzer/current && \
    mv /app/analyzer/current/lib/analyzer-*.jar /app/analyzer/current/lib/analyzer.jar && \
    dos2unix /usr/local/bin/* && \
    dos2unix /app/analyzer/current/config/* && \
    ls -R /app/analyzer


WORKDIR /app

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

CMD ["/bin/bash"]