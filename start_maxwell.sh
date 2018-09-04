#!/bin/bash

export JAVA_HOME=/home/vcap/app/.java-buildpack/open_jdk_jre
export PATH=$JAVA_HOME/bin:$PATH

./bin/maxwell \
  --user=$MAXWELL_USER \
  --password=$MAXWELL_PASSWORD \
  --host=$MYSQL_HOST \
  --producer=elasticsearch \
  --elastic_url=$ELASTIC_URL

