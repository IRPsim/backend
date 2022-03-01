#!/bin/bash

# Dieses Skript startet den Infobright-Datenbankserver auf einem zufälligen Port, der anschließend über stoptestdb.sh beendet werden kann. 
# Die URL des Servers wird in url.txt geschrieben, der Name in db.txt. Die URL kann aus Java heraus zum Verbinden genutzt werden.

# Sucht einen freien Port zwischen 9000 und 10000.
function getPort {
  port=$(( 9000 ))
  quit=0 

  while [ "$quit" -ne 1 ]; do
    # echo "Teste $port"
    netstat -na | grep $port >> /dev/null
    if [ $? -gt 0 ]; then
      quit=1
    else
      port=`expr $port + 1`
    fi
  done
  echo $port;
}

if [ "$#" -ne 1 ]; then
	echo "Es muss der Pfad für Import-Daten übergeben werden."
	exit
fi

port=$(getPort)

mkdir $1

success=1
while [ "$success" -ne 0 ]; do
	name="Testdb_$port"
    echo "Starte $name auf $port"
    echo $name > db.txt
    
    docker run -d --name=$name \
		--volume $1:/var/lib/import \
		-e MYSQL_ROOT_PASSWORD=test123 \
		-e MYSQL_DATADIR=/mnt/mysql_data \
		-e MYSQL_DATABASE=irpsim_test \
		-e MYSQL_INITDB_SKIP_TZINFO=1 \
		-p $port:3306 mariadb:10.4
     success=$?
     if [ "$success" -ne 0 ]; then
     	port=`expr $port + 1`
     fi
done
	
sleep 5s

# DB muss nicht mehr erstellt werden, wenn Umgebungsvariable gesetzt ist
# mysql -u root -ptest123 --protocol=TCP -P $port -e "CREATE DATABASE irpsim_test"
	
export IRPSIM_MYSQL_USER_TEST=root
export IRPSIM_MYSQL_PASSWORD_TEST=test123
export IRPSIM_MYSQL_URL_TEST=jdbc:mysql://localhost:$port/irpsim_test

echo $IRPSIM_MYSQL_URL_TEST > url.txt
