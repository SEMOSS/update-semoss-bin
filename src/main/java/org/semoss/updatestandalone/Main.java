package org.semoss.updatestandalone;

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
	
	public static void main(String[] args) throws Exception {
		try (Scanner scanner = new Scanner(System.in)) {
			
			// Get the version from the user
			System.out.print("Enter the version: ");
			String version = scanner.nextLine();
			System.out.println("Updating to version " + version + ".");
			System.out.println();

			// Get the working directory from the user
			String defaultWorkingDirectory = System.getProperty("user.home") + FS + "semoss_updates";
			System.out.print("Enter the location of your working directory (or [Enter] for " + defaultWorkingDirectory + "): ");
			String workingDirectory = scanner.nextLine();
			if (workingDirectory.length() == 0) {
				workingDirectory = defaultWorkingDirectory;
			}
			if (workingDirectory.endsWith("\\") || workingDirectory.endsWith("/")) {
				workingDirectory = workingDirectory.substring(0, workingDirectory.length() - 1);
			}
			
			// Set the working directory
			Path workingDirectoryPath = Paths.get(workingDirectory);
			if (!Files.exists(workingDirectoryPath)) {
				Files.createDirectory(workingDirectoryPath);
			}			
			System.out.println("Using the working directory " + workingDirectory + ".");
			System.out.println();
			
			// Download artifacts
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
			CompletionService<String> completionService = new ExecutorCompletionService<>(executorService);
			for (Callable<String> artifactExtractor : artifactExtractors) {
				completionService.submit(artifactExtractor);
			}
			
			// Continue until all have completed
			while (expectedReturns.size() > 0) {
				try {
					System.out.println("Waiting on " + expectedReturns.size() + " artifact(s).");
					String name = completionService.take().get();
					String extractedArtifactPath = ArtifactExtractor.getExtractedArtifactPathFromName(workingDirectory, name);
					System.out.println("Sucessfully extracted " + name + " to " + extractedArtifactPath + ".");
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
			
			// Shutdown the service
			executorService.shutdownNow();
			System.out.println("Complete.");
		}
	}
		

}
