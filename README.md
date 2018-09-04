<div id="maxwell-header">
</div>

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

