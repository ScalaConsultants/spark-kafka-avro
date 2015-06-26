# spark-kafka-avro
POC: Spark consumer for bottledwater-pg Kafka Avro topics

This is a proof of concept of streaming a whole Postgres database with [Bottled Water](http://blog.confluent.io/2015/04/23/bottled-water-real-time-integration-of-postgresql-and-kafka/) to Kafka and then to consume the Kafka topics in Spark.

Bottled Water
-------------
Bottled Water uses the [logical decoding](http://www.postgresql.org/docs/9.4/static/logicaldecoding.html)
feature (introduced in PostgreSQL 9.4) to extract a consistent snapshot and a continuous stream
of change events from a database. The data is extracted at a row level, and encoded using
[Avro](http://avro.apache.org/). A client program connects to your database, extracts this data,
and relays it to [Kafka](http://kafka.apache.org/) (you could also integrate it with other systems
if you wish, but Kafka is pretty awesome).

Key features of Bottled Water are:

* Works with any PostgreSQL database (version 9.4 or later). There are no restrictions on your
  database schema.
* No schema changes are required, no triggers or additional tables. (However, you do need to be
  able to install a PostgreSQL extension on the database server. More on this below.)
* Negligible impact on database performance.
* Transactionally consistent output. That means: writes appear only when they are committed to the
  database (writes by aborted transactions are discarded), writes appear in the same order as they
  were committed (no race conditions).
* Fault-tolerant: does not lose data, even if processes crash, machines die, the network is
  interrupted, etc.

Prerequisites
-------------
The whole environment will run in Docker. So you must install Docker on your workstation. Check out the installation instructions at https://docs.docker.com

Running in Docker
-----------------
Start zookeeper:

    $ docker run -d --name zookeeper --hostname zookeeper confluent/zookeeper
    
Start Avro schema-registry:

    $ docker run -d --name schema-registry --hostname schema-registry \
        --link zookeeper:zookeeper --link kafka:kafka \
        --env SCHEMA_REGISTRY_AVRO_COMPATIBILITY_LEVEL=none confluent/schema-registry

Start Postgres server and map the container port 5432 to 32768 on your local machine:

    $ docker run -d -p 32768:5432 --name postgres --hostname postgres confluent/postgres-bw:0.1
    
> Note: Note: If you have used the boot2docker virtual machine on OS X, Windows or Linux, you’ll need to get the IP of the virtual host instead of using localhost. You can do this by running the following outside of the boot2docker shell (i.e., from your comment line or terminal application).

    $ boot2docker ip
    
The `postgres-bw` image extends the
[official Postgres docker image](https://registry.hub.docker.com/_/postgres/) and adds
Bottled Water support. However, before Bottled Water can be used, it first needs to be
enabled. To do this, start a `psql` shell for the Postgres database:

    $ docker run -it --rm --link postgres:postgres postgres:9.4 sh -c \
        'exec psql -h "$POSTGRES_PORT_5432_TCP_ADDR" -p "$POSTGRES_PORT_5432_TCP_PORT" -U postgres'

When the prompt appears, enable the `bottledwater` extension:

    create extension bottledwater;
    CREATE DATABASE ds2;
    \q
    
To have some data to play with we will import a modified example [Dell DVD Store](http://linux.dell.com/dvdstore/) database into Postgres. The database dump can be found in `postgres/ds2.backup`. The easiest way to import the dump is to install [pgAdmin](http://pgadmin.org). Alternatively you can import the dump from your terminal (requires Postgres on your local machine):

    $ pg_restore --dbname=ds2 --host=<ip> --port=32768 -U postgres postgres/ds2.backup
    
The next step is to start the Bottled Water client, which relays data from Postgres to Kafka. 

    $ docker run -d --name bottledwater --hostname bottledwater --link postgres:postgres \
        --env POSTGRES_DBNAME=ds2 --env POSTGRES_USER=postgres \
        --link kafka:kafka --link schema-registry:schema-registry sibex/bottledwater:0.1

Bottled Water takes the snapshot, and continues to watch Postgres for any data changes. You can see the data
that has been extracted from Postgres by consuming from Kafka:

    $ docker run -it --rm --link zookeeper:zookeeper --link kafka:kafka \
        --link schema-registry:schema-registry confluent/tools \
        kafka-avro-console-consumer --property print.key=true --topic categories --from-beginning

Now it's time to build the `Spark Streaming` application:

    $ sbt assembly
    
> Note: For the sake of development speed you have to download [Spark](http://d3kbcqa49mib13.cloudfront.net/spark-1.4.0-bin-hadoop2.4.tgz) and extract it to the `spark` folder in the main directory. The `Dockerfile` assumes the folder `spark/spark-1.4.0-bin-hadoop2.4` to be there. The `Dockerfile` can be later changed to download `Spark` and extract each time the `Docker` image gets builded.

Build the `Docker` image with the deployed `Spark Streaming` application (JAR):

    $ docker build -f docker/Dockerfile -t spark-kafka-avro .

Finally run the application:

    $ docker run -it --rm --name spark --hostname spark --link postgres:postgres \
        --env POSTGRES_DBNAME=ds2 --env POSTGRES_USER=postgres \
        --link kafka:kafka --link schema-registry:schema-registry spark-kafka-avro

Before creating another `Docker` image the previous one should be removed
    
    $ docker rmi spark-kafka-avro
