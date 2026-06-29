#!/bin/bash
sudo java -Dproperty.file.path="$(pwd)/update.properties" \
  -Dworking.directory="$(pwd)/wd" \
  -classpath updatesemoss-*.jar \
  org.semoss.updatesemoss.Main
