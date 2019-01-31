package org.semoss.updatestandalone;

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
	private final String extractedArtifactPath;
	
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
		
		artifactUrl = UpdateUtil.SONATYPE_PREFIX + artifactId + "/" + version + "/" + name + packaging.getExtension();
		
		artifactPath = workingDirectory + FS + name + packaging.getExtension();
		
		// Since the tar.gz packaging contains a sub directory
		extractToPath = packaging.equals(Packaging.TAR_GZ)
				? workingDirectory
				: workingDirectory + FS + artifactId + "-" + version;
		extractedArtifactPath = getExtractedArtifactPathFromName(workingDirectory, name);
	}

	@Override
	public String call() throws Exception {
		System.out.println("Downloading " + name + "...");
		UpdateUtil.downloadFile(artifactUrl, artifactPath);
		System.out.println("Extracting " + name + "...");
		UpdateUtil.extractFile(artifactPath, extractToPath, packaging);
		UpdateUtil.deleteFile(artifactPath);
		return name;
	}
	
	public String getName() {
		return name;
	}
	
	public String getExtractedArtifactPath() {
		return extractedArtifactPath;
	}
	
	public static String getExtractedArtifactPathFromName(String workingDirectory, String name) {
		String[] splitName = name.split("-");
		String artifactId = splitName[0];
		String version = splitName[1];
		return workingDirectory + FS + artifactId + "-" + version;
	}
	
}
