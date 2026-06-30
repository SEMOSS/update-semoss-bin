# update-semoss-bin

Small utility project to build and run the SEMOSS updater fat JAR.

The updater downloads SEMOSS artifacts (SemossHome, Monolith, SemossWeb) from Maven repositories and extracts them into a local working directory (wd) before optional deployment steps.

## What this project contains

- Java source for the updater CLI
- Maven build that produces a fat JAR with dependencies
- Launcher scripts for Linux and Windows
- update.properties for target directory configuration

## Prerequisites

- Java 8 (current project target)
- Maven 3.x
- Network access to the SEMOSS artifact repositories

## Build the fat JAR

From the project root:

```bash
mvn clean package
```

Expected output JAR:

```bash
target/updatesemoss-0.0.1-SNAPSHOT-jar-with-dependencies.jar
```

## Prepare a runnable folder

```bash
mkdir -p update-semoss/wd
cp target/updatesemoss-0.0.1-SNAPSHOT-jar-with-dependencies.jar update-semoss/
cp scripts/* update-semoss/
cp update.properties update-semoss/
cd update-semoss/
```

## Download components into wd

Run the Linux launcher and answer prompts:

```bash
./update_semoss.sh
```

Example non-interactive run (SNAPSHOT):

```bash
./update_semoss.sh << 'EOF'
yes
version
5.4.0-SNAPSHOT
yes
no
EOF
```

What these answers do:

- yes confirm paths from update.properties
- version choose version mode
- 5.4.0-SNAPSHOT version to download
- yes download artifacts
- no skip code replacement step

After download, extracted artifacts will be staged under:

```bash
./wd
```

## Notes

- Edit update.properties to match your environment paths.
- If using release versions, replace the version value (for example 5.3.0).
