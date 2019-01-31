package org.semoss.updatestandalone;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;
import org.semoss.updatestandalone.ArtifactExtractor.Packaging;

public class UpdateUtil {
	
	public static final String SONATYPE_PREFIX = "https://oss.sonatype.org/content/groups/public/org/semoss/";
	
	public static void downloadFile(String fileUrl, String filePath) throws IOException {
		try (ReadableByteChannel readableByteChannel = Channels.newChannel(new URL(fileUrl).openStream());
				FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
			fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
		}
	}
	
	public static void extractFile(String archivePath, String destinationPath, Packaging packaging) throws IOException {
		File archive = new File(archivePath);
		File destination = new File(destinationPath);

		// Since the only two types allowed are tar.gz or war, this is OK
		Archiver archiver = packaging.equals(Packaging.TAR_GZ)
				? ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
				: ArchiverFactory.createArchiver(ArchiveFormat.JAR);
		archiver.extract(archive, destination);
	}
	
	public static void deleteFile(String filePath) throws IOException {
		Files.deleteIfExists(Paths.get(filePath));
	}
	
}
