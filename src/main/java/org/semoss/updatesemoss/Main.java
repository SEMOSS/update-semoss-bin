package org.semoss.updatesemoss;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
		
	private static final String FS = System.getProperty("file.separator");
	
	private static final String WORKING_DIRECTORY_KEY = "working.directory";
	
	private static final String STANDALONE_DIRECTORY_KEY = "standalone.directory";
	private static final String SEMOSS_HOME_DIRECTORY_KEY = "semoss.home.directory";
	private static final String MONOLITH_DIRECTORY_KEY = "monolith.directory";
	private static final String SEMOSS_WEB_DIRECTORY_KEY = "semoss.web.directory";

	public static void main(String[] args) throws Exception {
		
		String workingDirectory = System.getProperty(WORKING_DIRECTORY_KEY);
		String standaloneDirectory = Props.getInstance().getProperty(STANDALONE_DIRECTORY_KEY);
		String semossHomeDirectory = Props.getInstance().getProperty(SEMOSS_HOME_DIRECTORY_KEY);
		String monolithDirectory = Props.getInstance().getProperty(MONOLITH_DIRECTORY_KEY);
		String semossWebDirectory = Props.getInstance().getProperty(SEMOSS_WEB_DIRECTORY_KEY);
		
		System.out.println("Working directory is located in: " + workingDirectory);
		System.out.println("Standalone is located in:        " + standaloneDirectory);
		System.out.println("semosshome is located in:        " + semossHomeDirectory);
		System.out.println("Monolith is located in:          " + monolithDirectory);
		System.out.println("SemossWeb is located in:         " + semossWebDirectory);

		try (Scanner scanner = new Scanner(System.in)) {
			
			// Continue
			System.out.print("Is this information correct? [yes/no]: ");
			String correct = scanner.nextLine();
			if (!correct.equalsIgnoreCase("yes")) {
				return;
			}
			
			// Get the version from the user
			System.out.print("Enter the version: ");
			String version = scanner.nextLine();
			System.out.println("Updating to version " + version + ".");
			
			// Whether to download
			System.out.print("Do you want to download files? [yes/no]: ");
			boolean download = scanner.nextLine().equalsIgnoreCase("yes");
			
			// Whether to update
			System.out.print("Do you want to update code? [yes/no]: ");
			boolean update = scanner.nextLine().equalsIgnoreCase("yes");
			
			// Set the working directory
			Path workingDirectoryPath = Paths.get(workingDirectory);
			if (!Files.exists(workingDirectoryPath)) {
				Files.createDirectory(workingDirectoryPath);
			}

			// Download artifacts
			if (download) {
				List<Callable<String>> artifactExtractors = new ArrayList<>();
				Set<String> expectedReturns = new HashSet<>();
				
				// Add home
				ArtifactExtractor homeExtractor = new ArtifactExtractor(workingDirectory, "semoss", version, "semosshome", ArtifactExtractor.Packaging.TAR_GZ);
				artifactExtractors.add(homeExtractor);
				expectedReturns.add(homeExtractor.getName());
				
				// Add lib
				ArtifactExtractor libExtractor = new ArtifactExtractor(workingDirectory, "monolith", version, "libraries", ArtifactExtractor.Packaging.TAR_GZ);
				artifactExtractors.add(libExtractor);
				expectedReturns.add(libExtractor.getName());
				
				// Add war
				ArtifactExtractor warExtractor = new ArtifactExtractor(workingDirectory, "monolith", version, null, ArtifactExtractor.Packaging.WAR);
				artifactExtractors.add(warExtractor);
				expectedReturns.add(warExtractor.getName());
				
				// Add web
				ArtifactExtractor webExtractor = new ArtifactExtractor(workingDirectory, "semossweb", version, null, ArtifactExtractor.Packaging.WAR);
				artifactExtractors.add(webExtractor);
				expectedReturns.add(webExtractor.getName());
				
				// Submit the tasks
				ExecutorService executorService = Executors.newFixedThreadPool(4);
				try {
					CompletionService<String> completionService = new ExecutorCompletionService<>(executorService);
					for (Callable<String> artifactExtractor : artifactExtractors) {
						completionService.submit(artifactExtractor);
					}
					
					// Continue until all have completed
					while (expectedReturns.size() > 0) {
						try {
							String name = completionService.take().get();
							expectedReturns.remove(name);
						} catch (InterruptedException e) {
							e.printStackTrace();
							executorService.shutdownNow();
							Thread.currentThread().interrupt(); // Preserve interrupt status
							return;
						} catch (ExecutionException e) {
							e.printStackTrace();
							executorService.shutdownNow();
							return;
						}
					}
				} finally {
					
					// Shutdown the service
					executorService.shutdownNow();
				}
			}
			
			// Update code
			if (update) {
				
				// Delete old code
				UpdateUtil.deleteDirectoryContentsExcept(semossHomeDirectory, "removing existing semosshome", "db", "RDF_Map.prop", "social.properties", "rpa", "portables");
				UpdateUtil.deleteDirectoryContentsExcept(monolithDirectory, "removing existing Monolith", "WEB-INF/web.xml");
				UpdateUtil.deleteDirectoryContentsExcept(semossWebDirectory, "removing existing SemossWeb", "app.constants.js");
				
				String extractedHomePath = ArtifactExtractor.getExtractedPath(workingDirectory, "semoss", version);
				String extractedWarPath = ArtifactExtractor.getExtractedPath(workingDirectory, "monolith", version);
				String extractedWebPath = ArtifactExtractor.getExtractedPath(workingDirectory, "semossweb", version);
				
				// Update with new code
				UpdateUtil.copyDirectoryContentsExcept(extractedHomePath, semossHomeDirectory, "db", "RDF_Map.prop", "social.properties", "rpa", "portables");
				UpdateUtil.copyDirectoryContentsExcept(extractedWarPath, monolithDirectory, "WEB-INF/web.xml");
				UpdateUtil.copyDirectoryContentsExcept(extractedWebPath, semossWebDirectory);
				
				// Write the version
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(standaloneDirectory + FS + "version.txt"))) {
					writer.write(version);
				}
				
				// Cleanup
				UpdateUtil.deleteDirectoryContents(workingDirectory);
			}
			System.out.println("Complete.");
		}
	}

}
