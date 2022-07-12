package org.semoss.updatesemoss;

import java.util.concurrent.Callable;

public class ArtifactExtractor implements Callable<String>  {
	
	public enum Packaging {
		TAR_GZ(".tar.gz"), WAR(".war");
		
		private final String extension;
		
		private Packaging(String extension) {
			this.extension = extension;
		}
		
		public String getExtension() {
			return extension;
		}
	}
	
	private static final String FS = System.getProperty("file.separator");
	
	private final Packaging packaging;
	private final String name;
	private final String artifactUrl;
	private final String artifactPath;
	private final String extractToPath;
	private final String extractedPath;
	
	public ArtifactExtractor(String workingDirectory, String artifactId, String version, String classifier, Packaging packaging) throws Exception {
		this.packaging = packaging;

		// The way the name works is 
		// <artifact id>-<version>-[<classifier>]                                <--- Releases
		// <artifact id>-<version>-<timestamp>-<build number>-[<classifier>]     <--- SNAPSHOTS
		StringBuilder nameBuilder = new StringBuilder();
		nameBuilder.append(artifactId).append("-");
		if (version.endsWith("SNAPSHOT")) {
			
			// Remove the SNAPSHOT, as this is replaced with time and build
			nameBuilder.append(version.replaceAll("SNAPSHOT", ""));
			
			// Parse the metadata
			MavenMetadataParser parser = new MavenMetadataParser(workingDirectory, artifactId, version);
			String timestamp = parser.getValue("versioning", "snapshot", "timestamp");
			String buildNumber = parser.getValue("versioning", "snapshot", "buildNumber");

			// Add the time and build
			nameBuilder.append(timestamp).append("-");
			nameBuilder.append(buildNumber);			
		} else {
			nameBuilder.append(version);
		}
		if (classifier != null) nameBuilder.append("-").append(classifier);
		
		this.name = nameBuilder.toString();
		
		
		artifactUrl = UpdateUtil.SONATYPE_PREFIX + artifactId + "/" + sanitzeVersion(version) + "/" + name + packaging.getExtension();
		
		artifactPath = workingDirectory + FS + name + packaging.getExtension();
	
		extractedPath = getExtractedPath(workingDirectory, artifactId, version);
		
		// Since the tar.gz packaging contains a sub directory
		extractToPath = packaging.equals(Packaging.TAR_GZ)
				? workingDirectory
				: extractedPath;
	}

	@Override
	public String call() throws Exception {
		UpdateUtil.downloadFile(artifactUrl, artifactPath);
		UpdateUtil.extractFile(artifactPath, extractToPath, packaging);
		UpdateUtil.deleteFile(artifactPath);
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	public String getExtractedPath() {
		return extractedPath;
	}
	
	public static String getExtractedPath(String workingDirectory, String artifactId, String version) {
		return workingDirectory + FS + artifactId + "-" + sanitzeVersion(version);
	}
	
	public static String sanitzeVersion(String version) {
		if(version.contains("-")) {
			StringBuilder versionBuilder = new StringBuilder();
			versionBuilder.append(version.substring(0, version.indexOf("-")));
			versionBuilder.append("-SNAPSHOT");
			 version = versionBuilder.toString();
		}
		return version;
	}
	
}
