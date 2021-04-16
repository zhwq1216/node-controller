FROM metersphere/fabric8-java-alpine-openjdk8-jre

MAINTAINER FIT2CLOUD <support@fit2cloud.com>

ARG MS_VERSION=dev

RUN mkdir -p /opt/apps && mkdir -p /opt/jmeter/lib/junit

ADD target/node-controller-1.8.jar /opt/apps

COPY target/classes/jmeter/ /opt/jmeter/

ENV JAVA_APP_JAR=/opt/apps/node-controller-1.8.jar

ENV AB_OFF=true

ENV MS_VERSION=${MS_VERSION}

ENV JAVA_OPTIONS=-Dfile.encoding=utf-8

CMD ["/deployments/run-java.sh"]
