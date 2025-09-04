#!/bin/bash
sudo java -Dproperty.file.path="$(pwd)/update.properties" \
  -Dworking.directory="$(pwd)/wd" \
  -classpath update-semoss-*.jar \
  org.semoss.updatesemoss.Main
