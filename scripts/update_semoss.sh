sudo java -Dproperty.file.path="$(pwd)/update.properties" -Dworking.directory="$(pwd)/wd" -classpath updatesemoss-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.semoss.updatesemoss.Main
