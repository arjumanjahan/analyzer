FROM bde2020/spark-base:2.4.3-hadoop2.7

MAINTAINER Durga Haridoss

USER root

RUN yum -y install dos2unix net-tools tar rsync

RUN mkdir -p /app && mkdir -p /app/output && mkdir -p /app/log && \
    chmod  -R 777 /app/log && chmod  -R 777 /app/output

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