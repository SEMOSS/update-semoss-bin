package org.semoss.updatesemoss;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
			pb.stepTo(pb.getMax());
		}
	}
	
	public static void extractFile(String archivePath, String destinationPath, Packaging packaging) throws IOException {
		switch (packaging) {
		case TAR_GZ:
			extractTarGz(archivePath, destinationPath);
			break;
		case WAR:
			extractWar(archivePath, destinationPath);
			break;
		default:
			throw new IllegalArgumentException("Unable to extract archive with the packaging: " + packaging);
		}
	}

	private static void extractTarGz(String archivePath, String destinationPath) throws IOException {
		
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
			pb.stepTo(pb.getMax());
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
			pb.stepTo(pb.getMax());
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
			pb.stepTo(pb.getMax());
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
	
	public static void copyFile(String sourcePath, String targetPath) throws IOException {
		Files.copy(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
	}
		
	public static void deleteDirectory(String directoryPath) throws IOException {
		Files.walk(Paths.get(directoryPath))
			.map(Path::toFile)
			.sorted(Comparator.reverseOrder())
			.forEach(f -> {
				f.delete(); 
			});
	}
	
	public static void copyDirectory(String sourcePath, String targetPath) throws IOException {
		Path path = Paths.get(sourcePath);
		Files.walk(Paths.get(sourcePath))
			.map(Path::toFile)
			.sorted(Comparator.reverseOrder())
			.forEach(f -> {
				
				// Calculate where the file should go
				String relativePath = path.relativize(f.toPath()).toString();
				String absolutePath = Paths.get(targetPath, relativePath).toAbsolutePath().toString();
				
				// Make directories that don't yet exist
				File targetFile = new File(absolutePath);
				if (f.isDirectory()) {
					targetFile.mkdirs();
				} else {
					targetFile.getParentFile().mkdirs();
					
					// Finally, copy the file
					try {
						copyFile(f.getAbsolutePath(), Paths.get(targetPath, relativePath).toAbsolutePath().toString());
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					} 
				}
			});
	}
		
	public static void deleteDirectoryContents(String directoryPath) throws IOException {
		deleteDirectoryContentsExcept(directoryPath);
	}
	
	public static void copyDirectoryContents(String sourcePath, String targetPath) throws IOException {
		copyDirectoryContentsExcept(sourcePath, targetPath);
	}
	
	public static void deleteDirectoryContentsExcept(String directoryPath, String... omit) throws IOException {
		Path path = Paths.get(directoryPath);
		String directoryName = path.getFileName().toString();
		
		long size = getSizeOfDirectoryContents(directoryPath, omit);
		
		try (ProgressBar pb = new ProgressBar(formatName("removing contents of " + directoryName), size, ProgressBarStyle.ASCII)) {
			getFilteredStream(directoryPath, omit)
				.sorted(Comparator.reverseOrder())
				.forEach(f -> {
					f.delete(); 
					pb.stepBy(1);
				});
			pb.stepTo(pb.getMax());
		}
	}
	
	public static void copyDirectoryContentsExcept(String sourcePath, String targetPath, String... omit) throws IOException {
		Path path = Paths.get(sourcePath);
		String directoryName = path.getFileName().toString();
		
		long size = getSizeOfDirectoryContents(sourcePath, omit);
		
		try (ProgressBar pb = new ProgressBar(formatName("copying contents of " + directoryName), size, ProgressBarStyle.ASCII)) {
			getFilteredStream(sourcePath, omit)
				.sorted(Comparator.reverseOrder())
				.forEach(f -> {
					
					// Calculate where the file should go
					String relativePath = path.relativize(f.toPath()).toString();
					String absolutePath = Paths.get(targetPath, relativePath).toAbsolutePath().toString();
					
					// Make directories that don't yet exist
					File targetFile = new File(absolutePath);
					if (f.isDirectory()) {
						targetFile.mkdirs();
					} else {
						targetFile.getParentFile().mkdirs();
						
						// Finally, copy the file
						try {
							copyFile(f.getAbsolutePath(), Paths.get(targetPath, relativePath).toAbsolutePath().toString());
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						} 
					}
					pb.stepBy(1);
				});
			pb.stepTo(pb.getMax());
		}
	}
	
	private static long getSizeOfDirectoryContents(String directoryPath, String... omit) throws IOException {
		return getFilteredStream(directoryPath, omit).count();
	}
	
	private static Stream<File> getFilteredStream(String directoryPath, String... omit) throws IOException {
		List<String> omitList = Arrays.asList(omit).stream()
				.map(s -> {
					s = s.replace('\\', '/');
					if (s.endsWith("/")) {
						s = s.substring(0, s.length() - 1);
					}
					return s;
				}).collect(Collectors.toList());
		
		Path path = Paths.get(directoryPath);
		return Files.walk(Paths.get(directoryPath))
			.map(Path::toFile)
			.filter(f -> {
				Path relativePath = path.relativize(f.toPath());
				boolean isEmpty = relativePath.toString().isEmpty();
				boolean fileIsOmitted = omitList.contains(relativePath.toString().replace('\\', '/'));
				boolean parentDirectoryIsOmitted = relativePath.getParent() != null && omitList.contains(relativePath.getParent().toString().replace('\\', '/'));
				boolean isOmitted = fileIsOmitted || parentDirectoryIsOmitted;
				return !isEmpty && !isOmitted;
			});
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
		
		downloadFile(warFileUrl, warFilePath);
		copyFile(warFilePath, warFilePath + ".copy");
		extractFile(warFilePath, warDestinationPath, Packaging.WAR);
		copyDirectory(warDestinationPath, warDestinationPath + "_copy");
		deleteFile(warFilePath);
		deleteFile(warFilePath + ".copy");
		deleteDirectoryContents(warDestinationPath);
		deleteDirectory(warDestinationPath + "_copy");
		
		String tarGzFileUrl = "https://oss.sonatype.org/content/groups/public/org/semoss/semoss/3.3.7/semoss-3.3.7-semosshome.tar.gz";
		String tarGzPath = "C:\\Users\\tbanach\\Documents\\Workspace\\update-semoss\\wd\\semoss-3.3.7-semosshome.tar.gz";
		String tarGzDestinationPath = "C:\\Users\\tbanach\\Documents\\Workspace\\update-semoss\\wd\\semosshome";
		
		downloadFile(tarGzFileUrl, tarGzPath);
		extractFile(tarGzPath, tarGzDestinationPath, Packaging.TAR_GZ);
		deleteFile(tarGzPath);
		copyDirectoryContentsExcept(tarGzDestinationPath + "\\semoss-3.3.7", tarGzDestinationPath + "\\semoss-3.3.7_copy0", "RDF_Map.prop", "db/security/");
		copyDirectoryContentsExcept(tarGzDestinationPath + "\\semoss-3.3.7", tarGzDestinationPath + "\\semoss-3.3.7_copy1", "RDF_Map.prop", "db/security");
		copyDirectoryContents(tarGzDestinationPath + "\\semoss-3.3.7", tarGzDestinationPath + "\\semoss-3.3.7_copy2");
		deleteDirectoryContentsExcept(tarGzDestinationPath + "\\semoss-3.3.7", "RDF_Map.prop", "db/security");
		
		deleteDirectoryContents("C:\\Users\\tbanach\\Documents\\Workspace\\update-semoss\\wd");
	}
	
}
