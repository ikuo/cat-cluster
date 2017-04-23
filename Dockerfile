FROM openjdk:8u111-jre

WORKDIR /opt/cat-cluster
COPY target/scala-2.12/cat-cluster-assembly-0.1.0-SNAPSHOT.jar cat-cluster.jar
EXPOSE 8080
RUN mkdir log

CMD ["java", "-jar", "cat-cluster.jar"]
