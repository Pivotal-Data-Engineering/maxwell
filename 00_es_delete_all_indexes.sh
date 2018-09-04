#!/bin/bash

. ./es_env.sh

curl -X DELETE "$es_url/_all"

