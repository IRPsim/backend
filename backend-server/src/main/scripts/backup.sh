#/bin/bash

id=$(docker ps | grep "5030->5030" | awk '{print $1}')
echo "test"
running=$(curl "localhost:8484/simulation/simulations?running=true")
echo "Running: $running"
size=${#running}
echo "Running: $size"
echo "test2"
while [ ${#running} -gt 2 ] 
  do
  echo "Warte auf Jobende..."
  sleep 2
  running=$(curl 'localhost:8484/simulation/simulations?running=true')
done

echo "Lege Datenbankbackup an. Altes Datenbankbackup wird überschrieben!"
echo "Stoppe Docker-Container: $id"
docker stop $id
sudo rsync /root/src/docker-infobright/data /root/src/docker-infobright/backupdata
echo "Starte Docker-Container $id wieder"
docker start $id

