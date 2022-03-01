#!/bin/bash

set -e

echo "Monitoring: $MONITORING"

if [[ ! -z "$MONITORING" &&  $MONITORING -eq 1 ]]
then
	if [ ! -d monitoring-logs ]; then mkdir monitoring-logs; fi
	java $RAM \
	-Dcom.sun.management.jmxremote \
	-Dcom.sun.management.jmxremote.ssl=false \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.port=1898 \
	-Dcom.sun.management.jmxremote.rmi.port=1898 \
	-Djava.library.path=nativelibs/ \
	-Dkieker.monitoring.writer.filesystem.FileWriter.customStoragePath=monitoring-logs \
	-cp "lib/*:classes/" \
	-XX:+HeapDumpOnOutOfMemoryError \
	-javaagent:kieker-1.15-SNAPSHOT-aspectj.jar \
	de.unileipzig.irpsim.server.ServerStarter $@
else
	java $RAM \
	-Dcom.sun.management.jmxremote \
	-Dcom.sun.management.jmxremote.ssl=false \
	-Dcom.sun.management.jmxremote.authenticate=false \
	-Dcom.sun.management.jmxremote.port=1898 \
	-Dcom.sun.management.jmxremote.rmi.port=1898 \
	-Djava.library.path=nativelibs/ \
	-cp "lib/*:classes/" \
	-XX:+HeapDumpOnOutOfMemoryError \
	de.unileipzig.irpsim.server.ServerStarter $@
fi

echo "Backend beendet"
echo $?
