#!/bin/bash

. ./es_env.sh

curl -X GET "$es_url/_search?pretty" -H 'Content-Type: application/json' -d'
{
    "query" : {
      "query_string": {
        "query": "ford prefect"
       }
    },
    "highlight": {
      "fields": {
        "*": {}
      }
    }
}
'
