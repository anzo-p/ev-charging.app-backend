FROM amazoncorretto:11.0.17

RUN yum -y install shadow-utils

ENV TINI_VERSION v0.19.0
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini /sbin/tini
RUN echo "93dcc18adc78c65a028a84799ecf8ad40c936fdfc5f2a57b1acda5a8117fa82c  /sbin/tini" | sha256sum -c - && \
    chmod +x /sbin/tini
RUN echo 'export $(strings /proc/1/environ | grep AWS_CONTAINER_CREDENTIALS_RELATIVE_URI)' >> /root/.profile **

USER root
RUN yum update && \
    yum clean all && \
    rm -rf /var/lib/apt/lists/* && \
    rm -rf /var/cache/yum

RUN adduser user -u 1000
ADD --chown=user:user . /app

WORKDIR /app
USER 1000

EXPOSE 8081
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["/app/bin/app-backend"]
