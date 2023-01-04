FROM openjdk:8
ADD eregister-gateway-1.0.0-SNAPSHOT.jar eregister-gateway-1.0.0-SNAPSHOT.jar
# RUN apt-get install vim
# RUN apt-get install telnet
RUN mkdir -p /var/log/eregister-gateway
CMD java -jar eregister-gateway-1.0.0-SNAPSHOT.jar
