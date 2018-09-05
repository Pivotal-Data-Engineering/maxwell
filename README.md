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

### Background
When using a MySQL DB instance, provided by the PCF _tile_, CRUD operations (minus the 'R' -- read)
will be tracked and synchronized into Elasticsearch.  The following **caveats** do apply:
- `TRUNCATE TABLE ...` will not clear out the Elasticsearch index.
- Similarly, `DROP TABLE ...` will not affect the Elasticsearch index.
- The tables in the database must have a primary key; a compound key is okay.

The following illustrates how to integrate with Elasticsearch.  The relationship between
objects in MySQL to objects in Elasticsearch is:

MySQL | Elasticsearch
--- | ---
database | index
table | type
primary key | `_id`

### Procedure
1. Create a MySQL service instance.  The resulting DB will, by convention, be named `service_instance_db`.
1. Once the provisioning process completes, follow [this procedure](./MySQL_Tile_Service_Instance_Setup.md)
   to enable Maxwell's Daemon to access the MySQL instance's _binlog_ data.  This may require assistance
   from the operations team.
1. Install the A9S Elasticsearch tile into Ops Manager (again, this would be the operations group).
1. Create an instance of the service ([reference](https://docs.pivotal.io/partners/a9s-elasticsearch/using.html))
  ```
  $ cf cs a9s-elasticsearch6 elasticsearch-single-small-ssl elastic
  ```
1. Wait until this process finishes.  You can monitor its progress using:
  ```
  $ cf service elastic
  ```
1. Copy `./manifest-TEMPLATE.yml` to `./manifest.yml`.
1. Edit `./manifest.yml` to suit your deployment.  This will require some MySQL related values.
1. Push the app to PCF, but don't start it yet:
   ```
   $ cf push --no-start
   ```
1. Bind the app to the Elasticsearch instance:
   ```
   $ cf bs maxwell elastic
   ```
1. Start the app and tail its logs:
   ```
   $ cf start maxwell && cf logs maxwell
   ```

## FIXME / TODO
- Solve the DNS issue:
  ```
  2018-09-05T14:22:19.40-0400 [APP/PROC/WEB/0] ERR 18:22:19,400 ERROR MaxwellElasticsearchProducer - Exception during put
  2018-09-05T14:22:19.40-0400 [APP/PROC/WEB/0] ERR com.mashape.unirest.http.exceptions.UnirestException: java.net.UnknownHostException: d9709b5.service.dc1.a9s-elasticsearch-consul: Name or service not known
  2018-09-05T14:22:19.40-0400 [APP/PROC/WEB/0] ERR 	at com.mashape.unirest.http.HttpClientHelper.request(HttpClientHelper.java:143) ~[unirest-java-1.4.9.jar:?]
  ```

## OPTIONAL steps
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

## Example Elasticsearch search

- Edit `./es_search.sh`, setting the value of `"query"` to match data your app will insert into MySQL.
- Run the sample search: `./es_search.sh`

## Handling TRUNCATE (TODO)

- https://github.com/zendesk/maxwell/issues/1012

