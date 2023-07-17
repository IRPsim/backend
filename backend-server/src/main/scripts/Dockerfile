FROM localhost:5000/gams:24.7.1

# Debugging-Werkzeuge installieren
RUN apt-get -qq update && apt install -qqy software-properties-common gnupg2 apt-utils
ENV DEBIAN_FRONTEND="noninteractive"
#RUN add-apt-repository "deb http://http.us.debian.org/debian bookworm non-free contrib main"
RUN apt clean
RUN apt-get -qq update && apt-get -qqy install wget vim graphviz gnuplot && apt-get -y autoremove

ENV PATH /usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/opt/gams:/opt/java/openjdk/bin/
ENV LD_GAMS_PATH /usr/lib/:/lib:/opt/gams
#ENV JAVA_HOME /usr/lib/jvm/java-11-openjdk-amd64/jre/bin/
ENV IRPSIM_MYSQL_PATH /var/lib/import/
ENV IRPSIM_MYSQL_JAVAPATH /var/dataimport/
ENV IRPSIM_INTERPOLATION POSTPROCESSING
ENV IRPACT_PATH /opt/backend/models/irpact/

EXPOSE 8282
WORKDIR /opt/backend

ADD . /opt/backend

ENTRYPOINT ["/opt/backend/startup.sh"]
