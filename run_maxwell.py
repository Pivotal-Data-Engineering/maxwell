#!/usr/bin/env python

import os
import json
import subprocess

# Key into VCAP_SERVICES for the Elasticsearch service (this is set in manifest.yml)
es_service_key = os.environ["ES_SERVICE_KEY"]

# Toggle which of the ES URLs we'll use based on this
VERIFY_ES_SSL_KEY = "VERIFY_ES_SSL";
es_verify_ssl = (VERIFY_ES_SSL_KEY in os.environ and "true" == os.environ[VERIFY_ES_SSL_KEY].lower())

# Environment variables needed to run the Java app (Maxwell's Daemon)
os.environ["JAVA_HOME"] = "/home/vcap/app/.java-buildpack/open_jdk_jre"
os.environ["PATH"] = os.environ["JAVA_HOME"] + "/bin" + ":" + os.environ["PATH"]

# Get the A9S Elasticsearch instance credentials
es_url = None
es_user = None
es_password = None
cacert = None
vcap_str = os.environ.get("VCAP_SERVICES")
if vcap_str is None:
  raise Exception("VCAP_SERVICES not found in environment variables (necessary for credentials)")
vcap = json.loads(vcap_str)
cacert_key = "cacrt"
if es_service_key in vcap:
  creds = vcap[es_service_key][0]["credentials"]
  if es_verify_ssl:
    es_url = str(creds["host"][0])
  else:
    es_url = str(creds["host_ip"][0])
  es_user = str(creds["username"])
  es_password = str(creds["password"])
  if cacert_key in creds:
    cacert = str(creds[cacert_key])
else:
  raise Exception("No A9S Elasticsearch instance bound to this app")

# Install the SSL cert into the Java keystore
if es_verify_ssl and cacert is not None:
  cacert_file = "/tmp/a9s_cacert"
  with open(cacert_file, "w") as out:
    out.write(cacert)
  cmd = ["keytool", "-keystore", os.environ["JAVA_HOME"] + "/lib/security/cacerts",
    "-importcert", "-alias", "A9SES", "-file", cacert_file, "-noprompt", "-storepass", "changeit"]
  output = subprocess.check_output(cmd)
  print output

# Exec the Maxwell startup script, with the required arguments
os.execlp("./bin/maxwell",
  "--output_ddl=true",
  "--user=" +  os.environ.get("MAXWELL_USER"),
  "--password=" + os.environ.get("MAXWELL_PASSWORD"),
  "--host=" + os.environ.get("MYSQL_HOST"),
  "--producer=elasticsearch",
  "--elastic_url=" + es_url,
  "--elastic_user=" + es_user,
  "--elastic_password=" + es_password)

