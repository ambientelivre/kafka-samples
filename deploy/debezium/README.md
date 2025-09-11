# Debezium com PostgreSQL

docker run --name postgres -p 5000:5432 -e POSTGRES_PASSWORD=postgres -e POSTGRES_HOST_AUTH_METHOD=trust debezium/postgres:15-alpine

docker run -it --rm --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 --security-opt seccomp=unconfined quay.io/debezium/zookeeper:3.2

docker run -it --rm --name kafka -p 9092:9092 --security-opt seccomp=unconfined --link zookeeper:zookeeper quay.io/debezium/kafka:3.2

docker run -it --name connect -p 8083:8083 --privileged -e GROUP_ID=1 -e CONFIG_STORAGE_TOPIC=my-connect-configs -e OFFSET_STORAGE_TOPIC=my-connect-offsets -e ADVERTISED_HOST_NAME=$(echo $DOCKER_HOST | cut -f3 -d'/' | cut -f1 -d':') --link zookeeper:zookeeper --link postgres:postgres --link kafka:kafka quay.io/debezium/connect:3.2

## Crie um database e uma batela
docker exec -it postgres psql -U postgres

CREATE DATABASE inventory_db;

\c inventory_db

CREATE TABLE my_table(id SERIAL PRIMARY KEY, name VARCHAR);

## Configure o Connector

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

## Consulte se o Connector esta rodando
curl -X GET localhost:8083/connectors/inventory_db-connector/status

## No PostgreSQL

INSERT INTO my_table (name) VALUES ('Marcio');
INSERT INTO my_table (name) VALUES ('Joao');
INSERT INTO my_table (name) VALUES ('Maria');


## liste as filas 

docker exec -it connect /kafka/bin/kafka-topics.sh --list --bootstrap-server kafka:9092

## Consuma os registros da fila

docker exec -it connect /kafka/bin/kafka-console-consumer.sh \
--bootstrap-server kafka:9092 \
--topic test.public.my_table \
--from-beginning



## Caso precise pode dar restart no COnnector
curl -X POST localhost:8083/connectors/inventory_db-connector/restart

## Crie uma nova tabela 
CREATE TABLE my_table2(id SERIAL PRIMARY KEY, name VARCHAR);
INSERT INTO my_table2(name) VALUES ('Marcio');

## Crie dados com transacao no DB

BEGIN; 
INSERT INTO my_table (name) VALUES ('Registro 1'); 
INSERT INTO my_table (name) VALUES ('Registro 2'); 
INSERT INTO my_table (name) VALUES ('Registro 3'); 
COMMIT;


WAL - Write-Ahead Log
ALTER TABLE my_table REPLICA IDENTITY FULL;

## adicione controle de transaçao

curl -X PUT -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/inventory_db-connector/config -d '
{
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
"database.history.kafka.topic": "schema-changes.inventory",
"transaction.metadata.kafka.topic": "transactions_metadata"
}
'

## somente uma tabela

curl -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d ' { "name": "read-connector-tjrn", "config": { "connector.class": "io.debezium.connector.postgresql.PostgresConnector", "tasks.max": "1", "database.hostname": "postgres", "database.port": "5432", "database.user": "postgres", "database.password": "postgres", "database.dbname" : "read","database.server.name": "dbserver1", "table.include.list": "public.my_table", "database.history.kafka.bootstrap.servers": "kafka:9092", "topic.prefix": "pgserver1", "database.history.kafka.topic": "schema-changes.inventory",
 "slot.name": "debezium_read_table" } }'





