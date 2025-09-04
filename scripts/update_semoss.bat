@echo off
java -Dproperty.file.path="%~dp0update.properties" -Dworking.directory="%~dp0wd" -classpath updatesemoss-0.0.1-SNAPSHOT-jar-with-dependencies.jar org.semoss.updatesemoss.Main
PAUSE