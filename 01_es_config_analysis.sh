#!/bin/bash

. ./es_env.sh

#echo "Installing phonetic analyzer ..."
#./elasticsearch-6.3.2/bin/elasticsearch-plugin install analysis-phonetic
#echo "OK"

echo "Configuring analyzer ..."
curl -X PUT "$es_url/service_instance_db?pretty" -H "Content-Type: application/json" -d'
{
  "settings": {
    "number_of_shards": 1,
    "analysis": {
      "filter": {
        "light_english_stemmer": {
          "type":       "stemmer",
          "language":   "light_english" 
        },
        "english_possessive_stemmer": {
          "type":       "stemmer",
          "language":   "possessive_english"
        },
        "my_metaphone": {
            "type": "phonetic",
            "encoder": "doublemetaphone",
            "replace": false
        }
      },
      "analyzer": {
        "default": {
          "tokenizer":  "standard",
          "filter": [
            "english_possessive_stemmer",
            "lowercase",
            "light_english_stemmer", 
            "asciifolding",
            "my_metaphone"
          ]
        }
      }
    }
  }
}'
echo "OK"

