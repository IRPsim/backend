apache=$(sudo docker ps | grep model-master | awk '{print $1}')
sudo docker inspect $apache | grep Gateway | head -n 1
