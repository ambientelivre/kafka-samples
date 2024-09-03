


sudo docker run --name postgres -p 5000:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_HOST_AUTH_METHOD=trust debezium/postgres

sudo docker run -it --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 --security-opt seccomp=unconfined debezium/zookeeper 

sudo docker run -it --name kafka -p 9092:9092 --link zookeeper:zookeeper debezium/kafka

sudo docker run -it --name connect -p 8083:8083 -e GROUP_ID=1 -e CONFIG_STORAGE_TOPIC=my-connect-configs -e OFFSET_STORAGE_TOPIC=my-connect-offsets -e ADVERTISED_HOST_NAME=$(echo $DOCKER_HOST | cut -f3 -d'/' | cut -f1 -d':') --link zookeeper:zookeeper --link postgres:postgres --link kafka:kafka debezium/connect
psql -h localhost -p 5000 -U postgre


CREATE DATABASE inventory_db;

CREATE TABLE my_table(id SERIAL PRIMARY KEY, name VARCHAR);


curl -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '
{
"name": "inventory_db-connector",
"config": {
"connector.class": "io.debezium.connector.postgresql.PostgresConnector",
"tasks.max": "1",
"database.hostname": "postgres",
"database.port": "5432",
"database.user": "postgres",
"database.password": "postgres",
"database.dbname" : "inventory_db",
"database.server.name": "dbserver1",
"database.whitelist": "inventory_db",
"database.history.kafka.bootstrap.servers": "kafka:9092",
"topic.prefix": "test",
"database.history.kafka.topic": "schema-changes.inventory"
}
}'







