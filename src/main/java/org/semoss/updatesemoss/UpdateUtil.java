package org.semoss.updatesemoss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.semoss.updatesemoss.ArtifactExtractor.Packaging;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

public class UpdateUtil {
	
	public static final String SONATYPE_PREFIX = "https://oss.sonatype.org/content/groups/public/org/semoss/";
	
	private static final int BUFFER_SIZE = 16384;
	
	private static final int NAME_LENGTH = 50;
	
	public static void downloadFile(String fileUrl, String filePath) throws IOException {
		
		// Get the total file size
		URL url = new URL(fileUrl);
		long fileSize = getFileSize(url);
		
		String fileName = Paths.get(filePath).getFileName().toString();
		
		// Now start download process
		try (ProgressBar pb = new ProgressBar(formatName("downloading " + fileName), fileSize, ProgressBarStyle.ASCII);
				InputStream inStream = url.openConnection().getInputStream(); 
				ReadableByteChannel in = Channels.newChannel(inStream);
				FileOutputStream outStream = new FileOutputStream(filePath);
				FileChannel out = outStream.getChannel()) {
			write(in, out, pb);
		}
	}
	
	public static void extractFile(String archivePath, String destinationPath, Packaging packaging) throws IOException {
		switch (packaging) {
		case TAR_GZ:
			extractTarGz2(archivePath, destinationPath);
			break;
		case WAR:
			extractWar(archivePath, destinationPath);
			break;
		default:
			throw new IllegalArgumentException("Unable to extract archive with the packaging: " + packaging);
		}
	}

	private static void extractTarGz2(String archivePath, String destinationPath) throws IOException {
		
		// The intermediate tar file
		String intermediatePath = archivePath.replaceAll(".tar.gz", ".tar");
		
		// Get the total file size
		File archiveFile = new File(archivePath);
		long archiveFileSize = getExtractedFileSize(archiveFile, Packaging.TAR_GZ);

		String archiveFileName = Paths.get(archivePath).getFileName().toString();
		
		// Now start extraction process
		try (ProgressBar pb = new ProgressBar(formatName("decompressing " + archiveFileName), archiveFileSize, ProgressBarStyle.ASCII);
				InputStream inStream = new FileInputStream(archiveFile);
				InputStream inGzipStream = new GZIPInputStream(inStream);
				ReadableByteChannel in = Channels.newChannel(inGzipStream);
				FileOutputStream outStream = new FileOutputStream(intermediatePath);
				FileChannel out = outStream.getChannel()) {
			write(in, out, pb);
		}

		// Get the total file size
		File intermediateFile = new File(intermediatePath);
		long intermediateFileSize = getFileSize(intermediateFile);

		String intermediateFileName = Paths.get(intermediatePath).getFileName().toString();
		
		// Now start extraction process
		try (ProgressBar pb = new ProgressBar(formatName("extracting " + intermediateFileName), intermediateFileSize, ProgressBarStyle.ASCII);
				InputStream inStream = new FileInputStream(intermediateFile);
				TarArchiveInputStream inTarStream = new TarArchiveInputStream(inStream);
				ReadableByteChannel in = Channels.newChannel(inTarStream)) {
			
			TarArchiveEntry entry = null;
			while ((entry = inTarStream.getNextTarEntry()) != null) {				
				File entryFile = Paths.get(destinationPath, entry.getName()).toFile();
				if (entry.isDirectory()) {
					entryFile.mkdirs();
				} else {
					entryFile.getParentFile().mkdirs();
					try (FileOutputStream outStream = new FileOutputStream(entryFile);
							FileChannel out = outStream.getChannel()) {
						write(in, out, pb);
					}				
				}
			}
		}
		deleteFile(intermediatePath);
	}
	
	private static void extractWar(String archivePath, String destinationPath) throws IOException {
		
		// Get the total file size
		File archiveFile = new File(archivePath);
		long archiveFileSize = getExtractedFileSize(archiveFile, Packaging.WAR);
		
		String archiveFileName = Paths.get(archivePath).getFileName().toString();
		
		// Now start extraction process
		try (ProgressBar pb = new ProgressBar(formatName("extracting " + archiveFileName), archiveFileSize, ProgressBarStyle.ASCII);
				InputStream inStream = new FileInputStream(archiveFile);
				JarInputStream inJarStream = new JarInputStream(inStream);
				ReadableByteChannel in = Channels.newChannel(inJarStream)) {
			
			JarEntry entry = null;
			while ((entry = inJarStream.getNextJarEntry()) != null) {				
				File entryFile = Paths.get(destinationPath, entry.getName()).toFile();
				if (entry.isDirectory()) {
					entryFile.mkdirs();
				} else {
					entryFile.getParentFile().mkdirs();
					try (FileOutputStream outStream = new FileOutputStream(entryFile);
							FileChannel out = outStream.getChannel()) {
						write(in, out, pb);
					}				
				}
			}
		}
	}
	
	private static void write(ReadableByteChannel in, FileChannel out, ProgressBar pb) throws IOException {

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
			pb.stepBy(bytesRead);

			// Read the next buffer of bytes
			bytesRead = in.read(buffer);
		}
	}
	
	public static void deleteFile(String filePath) throws IOException {
		Files.deleteIfExists(Paths.get(filePath));
	}
	
	public static void deleteDirectory(String directoryPath) throws IOException {
		String directoryName = Paths.get(directoryPath).getFileName().toString();
		long size = Files.walk(Paths.get(directoryPath)).count();
		try (ProgressBar pb = new ProgressBar("removing contents of " + directoryName, size, ProgressBarStyle.ASCII)) {
			Files.walk(Paths.get(directoryPath))
				.map(Path::toFile)
				.sorted(Comparator.reverseOrder())
				.forEach(f -> {
					f.delete(); 
					pb.stepBy(1);
				});
		}
	}
		
	public static void deleteDirectoryContents(String directoryPath) throws IOException {
		deleteDirectoryContentsExcept(directoryPath);
	}
	
	public static void deleteDirectoryContentsExcept(String directoryPath, String... omit) throws IOException {
		String directoryName = Paths.get(directoryPath).getFileName().toString();
		List<String> omitList = Arrays.asList(omit);
		File[] contents = new File(directoryPath).listFiles();
		try (ProgressBar pb = new ProgressBar("removing contents of " + directoryName, contents.length, ProgressBarStyle.ASCII)) {
			for (File content : contents) {
				if (!omitList.contains(content.getName())) {
					if (content.isFile()) {
						deleteFile(content.getAbsolutePath().toString());
					} else if (content.isDirectory()) {
						deleteDirectory(content.getAbsolutePath().toString());
					}
				}
				pb.stepBy(1);
			}
		}
		
	}
	
	private static long getFileSize(URL url) throws IOException {
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
	
	private static long getFileSize(File file) {
		return file.length();
	}
	
	private static long getExtractedFileSize(File file, Packaging packaging) throws IOException {
		switch (packaging) {
		case TAR_GZ:
			try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
				raf.seek(raf.length() - 4);
				int b4 = raf.read();
				int b3 = raf.read();
				int b2 = raf.read();
				int b1 = raf.read();
				long size = ((long)b1 << 24) | ((long)b2 << 16) | ((long)b3 << 8) | (long)b4;
				return size;
			}
		case WAR:
			long size = 0;
			try (JarFile jf = new JarFile(file)) {
				Enumeration<JarEntry> entries = jf.entries();
				while (entries.hasMoreElements()) {
					size += entries.nextElement().getSize();
				}
			}
			return size;
		default:
			throw new IllegalArgumentException("Unable to extract archive with the packaging: " + packaging);
		}
	}
	
	private static String formatName(String name) {
		int length = name.length();
		if (length < NAME_LENGTH) {
			int nAdd = NAME_LENGTH - length;
			for (int i = 0; i < nAdd; i++) {name += " ";}; 
		} else if (length > NAME_LENGTH) {
			name = name.substring(0, NAME_LENGTH - 3);
			name += "...";
		}
		return name;
	}
	

	public static void main(String[] args) throws Exception {
		
		String warFileUrl = "https://oss.sonatype.org/content/repositories/public/org/semoss/monolith/3.3.9.3/monolith-3.3.9.3.war";
		String warFilePath = "C:\\Users\\tbanach\\Documents\\Workspace\\update-semoss\\wd\\monolith-3.3.9.3.war";
		String warDestinationPath = "C:\\Users\\tbanach\\Documents\\Workspace\\update-semoss\\wd\\monolith";
		
		try {deleteDirectory(warDestinationPath);} catch (NoSuchFileException e) {}
		downloadFile(warFileUrl, warFilePath);
		extractFile(warFilePath, warDestinationPath, Packaging.WAR);
		deleteFile(warFilePath);
		deleteDirectoryContents(warDestinationPath);
		
		String tarGzFileUrl = "https://oss.sonatype.org/content/groups/public/org/semoss/semoss/3.3.7/semoss-3.3.7-semosshome.tar.gz";
		String tarGzPath = "C:\\Users\\tbanach\\Documents\\Workspace\\update-semoss\\wd\\semoss-3.3.7-semosshome.tar.gz";
		String tarGzDestinationPath = "C:\\Users\\tbanach\\Documents\\Workspace\\update-semoss\\wd\\semosshome";
		
		try {deleteDirectory(tarGzDestinationPath);} catch (NoSuchFileException e) {}
		downloadFile(tarGzFileUrl, tarGzPath);
		extractFile(tarGzPath, tarGzDestinationPath, Packaging.TAR_GZ);
		deleteFile(tarGzPath);
		deleteDirectoryContentsExcept("C:\\Users\\tbanach\\Documents\\Workspace\\update-semoss\\wd\\semosshome\\semoss-3.3.7", "RDF_Map.prop", "db");
	}
	
}
