FROM openjdk:8u111-jre

WORKDIR /opt/cat-cluster

# deprecated
ADD https://github.com/akka/akka/raw/v2.5.0/akka-cluster/jmx-client/jmxsh-R5.jar .
ADD https://raw.githubusercontent.com/akka/akka/v2.5.0/akka-cluster/jmx-client/akka-cluster .
RUN chmod a+x akka-cluster

COPY target/scala-2.12/cat-cluster-assembly-0.1.0-SNAPSHOT.jar cat-cluster.jar
COPY bin/startup.sh .
EXPOSE 8080
RUN mkdir log

CMD ["./startup.sh"]
