# Maxwell's Daemon

This is Maxwell's daemon, an application that reads MySQL binlogs and writes
row updates to Kafka, Kinesis, RabbitMQ, Google Cloud Pub/Sub, Redis (Pub/Sub
or LPUSH) **and Elasticsearch** as JSON.  Maxwell has a low operational bar and
produces a consistent, easy to ingest stream of updates.  It allows you to
easily "bolt on" some of the benefits of stream processing systems without
going through your entire code base to add (unreliable) instrumentation points.
Common use cases include ETL, cache building/expiring, metrics collection,
search indexing and inter-service communication.

- Can do `SELECT * from table` (bootstrapping) initial loads of a table.
- supports automatic position recover on master promotion
- flexible partitioning schemes for Kakfa - by database, table, primary key, or column
- Maxwell pulls all this off by acting as a full mysql replica, including a SQL
  parser for create/alter/drop statements (nope, there was no other way).

## Steps to run as an app on Pivotal Cloud Foundry (PCF)

When using a MySQL DB instance, provided by the PCF _tile_, all DB changes (so far,
INSERT / UPDATE / DELETE), will be handled -- TRUNCATE is *not*.  Given this, if it's
possible to use DELETE instead of TRUNCATE, that would be nice.

The following illustrates how to integrate with Elasticsearch.  The relationship between
objects in MySQL to objects in Elasticsearch is:

MySQL | Elasticsearch
--- | ---
database | index
table | type
primary key | `_id`

The operations carried out here rely on the tables having a primary key.

1. First, create a MySQL service instance.  The resulting DB will be named `service_instance_db`
1. You will also need access to an Elasticsearch service, potentially via the A9S Elasticsearch tile.
1. The numbered `00_es_*.sh` scripts are meant to be guides to configuring Elasticsearch.
1. Edit `./es_env.sh` to suit your deployment, then run `./01_es_config_analysis.sh` to set up the analysis chain for the index.
1. Once that process completes, follow [this procedure](./MySQL_Tile_Service_Instance_Setup.md) to enable Maxwell's Daemon to access the MySQL instance's _binlog_ data.
1. Copy `./manifest-TEMPLATE.yml` to `./manifest.yml`
1. Edit `./manifest.yml` to suit your deployment
1. Push the app to PCF: `cf push`

## Example Elasticsearch search

- Edit `./es_search.sh`, setting the value of `"query"` to match data your app will insert into MySQL.
- Run the sample search: `./es_search.sh`

## Working with an A9S Elasticsearch instance

- Install the A9S Elasticsearch tile into Ops Manager
- Create an instance of the service ([reference](https://docs.pivotal.io/partners/a9s-elasticsearch/using.html))
- Bind an app to this service instance
- Get the `VCAP_SERVICES` for this app, to locate the IP number and credentials
  for the Elasticsearch service:
  ```
  $ cf env whats_my_ip
  # Where whats_my_ip is the name of the bound app
  ```
- Create an SSH tunnel so it can be reached by your computer:
  ```
  $ cf ssh whats_my_ip -L 19200:10.0.12.21:9200
  # 10.0.12.21 is the IP of the Elasticsearch instance
  # and 19200 is the port you will use locally.
  ```
- Try to hit that endpoint (you will see a message that authorization is required):
  ```
  $ curl -k https://localhost:19200
  ```
- Try again, but using the `username` and `password` from `VCAP_SERVICES`:
  ```
  $ curl --user a9s4bf0da66afa506644f116985cad9464ec8277f8d:a9s4d4e82f320c365e55ef1d49e816d6e24f34240ea -k https://localhost:19200
  {
    "name" : "es/0",
    "cluster_name" : "d65d582",
    "cluster_uuid" : "a40v9s0AQcSnqd9T1YiSAA",
    "version" : {
      "number" : "5.6.9",
      "build_hash" : "877a590",
      "build_date" : "2018-04-12T16:25:14.838Z",
      "build_snapshot" : false,
      "lucene_version" : "6.6.1"
    },
    "tagline" : "You Know, for Search"
  }
  ```

## Handling TRUNCATE (TODO)

- https://github.com/zendesk/maxwell/issues/1012

