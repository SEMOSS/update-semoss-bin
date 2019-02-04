package org.semoss.updatesemoss;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;
import org.semoss.updatesemoss.ArtifactExtractor.Packaging;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

public class UpdateUtil {
	
	public static final String SONATYPE_PREFIX = "https://oss.sonatype.org/content/groups/public/org/semoss/";
	
	private static final int BUFFER_SIZE = 16384;
	
	public static void downloadFile(String fileUrl, String filePath, String name) throws IOException {
		
		// Get the total file size
		URL url = new URL(fileUrl);
		long fileSize = getFileSize(url);
		
		// Now start download process
		try (ProgressBar pb = new ProgressBar(name, fileSize, ProgressBarStyle.ASCII);
				ReadableByteChannel in = Channels.newChannel(url.openStream());
				FileOutputStream fos = new FileOutputStream(filePath);
				FileChannel out = fos.getChannel()) {
			
			// Allocate the buffer
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			
			// Read initial bytes
			int bytesRead = in.read(buffer);
			
			// Continue to read until there are no bytes left
			while (bytesRead != -1) {
				
				// Flip the buffer in order to write
				buffer.flip();
				
				// Write the bytes
				out.write(buffer);
				
				// Clear the buffer
				buffer.clear();

				// Progress info
				pb.stepBy(BUFFER_SIZE);
				
				// Read the next buffer of bytes
				bytesRead = in.read(buffer);
			}
		}
	}
	
	public static long getFileSize(URL url) throws IOException {
		String protocol = url.getProtocol();
		if (protocol.startsWith("http")) {
			HttpURLConnection conn = null;
		    try {
		        conn = (HttpURLConnection) url.openConnection();
		        conn.setRequestMethod("HEAD");
		        return conn.getContentLength();
		    } finally {
		    	conn.disconnect();
		    }
		} else {
			throw new IllegalArgumentException("The provided URL must be of the http protocol.");
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
