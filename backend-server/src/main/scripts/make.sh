#!/bin/bash

if [ $# -lt 1 ]; then
  echo "Typ des Projekts muss übergeben werden."
  exit 1
fi

docker build -t backend:$1 .
docker tag backend:$1 localhost:5000/backend:$1
docker push localhost:5000/backend:$1

