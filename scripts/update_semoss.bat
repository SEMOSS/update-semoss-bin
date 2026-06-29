@echo off
setlocal

REM Find the JAR filename matching pattern (update-semoss-v*.jar)
for %%f in (updatesemoss-*.jar) do set JAR=%%f

java -Dproperty.file.path="%~dp0update.properties" ^
     -Dworking.directory="%~dp0wd" ^
     -classpath "%JAR%" ^
     org.semoss.updatesemoss.Main

PAUSE
