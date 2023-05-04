FROM openjdk:20-ea-13-jdk-bullseye
MAINTAINER magno.mabreu@gmail.com
COPY ./target/cryptobrute-1.0.war /opt/lib/
ENTRYPOINT ["java"]
ENV LANG=pt_BR.utf8 
CMD ["-jar", "/opt/lib/cryptobrute-1.0.war"]