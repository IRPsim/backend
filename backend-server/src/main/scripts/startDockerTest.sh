docker build -t "backend-develop" .
docker run \
	--network irpsimtest \
	-e IRPSIM_MYSQL_USER=$IRPSIM_MYSQL_USER \
	-e IRPSIM_PERSISTENCEFOLDER=$IRPSIM_PERSISTENCEFOLDER \
	-e IRPSIM_MYSQL_PASSWORD=$IRPSIM_MYSQL_PASSWORD \
	-e IRPSIM_MYSQL_URL=jdbc:mysql://mariadb:3306/irpsim \
	--publish 8282:8282 \
	-d backend-develop
