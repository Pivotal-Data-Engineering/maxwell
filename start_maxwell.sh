#!/bin/bash

es_url="http://localhost:9200"
#es_url="http://localhost:18080"

bin/maxwell --output_nulls=false --user='maxwell' --password='maxwell' --host='127.0.0.1' --producer=elasticsearch --elastic_url=$es_url

