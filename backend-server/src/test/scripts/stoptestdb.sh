#!/bin/bash

# Stoppt den vorher mit starttestdb.sh gestarteten Server. Dafür wird der Server-Name aus db.txt genutzt - sollten mehrere Server vorhanden sein, müssen die anderen manuell beendet werden. 
# Außerdem wird der Container umbenannt, so dass ein neuer Container auf dem selben Port erstellt werden kann.

name=`cat db.txt`
docker stop $name

name2=$name
rename=1
echo $rename
count=0
while [[ "$rename" -ne 0 && "$count" -le 100 ]]; do
  name2=$name"_old_"$count
  echo "Benne um zu: $name2"
  docker rename $name $name2
  rename=$?
  echo "Rename: $rename $count"
  count=`expr $count + 1`
done
echo "Umbenennen abgeschlossen"