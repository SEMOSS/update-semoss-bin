package org.semoss.updatesemoss;

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
		
	private static final String WORKING_DIRECTORY_KEY = "working.directory";
	
	private static final String SEMOSS_HOME_DIRECTORY_KEY = "semoss.home.directory";
	private static final String MONOLITH_DIRECTORY_KEY = "monolith.directory";
	private static final String SEMOSS_WEB_DIRECTORY_KEY = "semoss.web.directory";

	public static void main(String[] args) throws Exception {
		
		String workingDirectory = System.getProperty(WORKING_DIRECTORY_KEY);
		String semossHomeDirectory = Props.getInstance().getProperty(SEMOSS_HOME_DIRECTORY_KEY);
		String monolithDirectory = Props.getInstance().getProperty(MONOLITH_DIRECTORY_KEY);
		String semossWebDirectory = Props.getInstance().getProperty(SEMOSS_WEB_DIRECTORY_KEY);
		
		System.out.println("Working directory is located in: " + workingDirectory);
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
			System.out.println();
			
			// Set the working directory
			Path workingDirectoryPath = Paths.get(workingDirectory);
			if (!Files.exists(workingDirectoryPath)) {
				Files.createDirectory(workingDirectoryPath);
			}
			
			// TODO >>>timb:
			UpdateUtil.deleteDirectoryContentsExcept(semossHomeDirectory, "removing existing semosshome", "db", "RDF_Map.prop", "social.properties", "rpa");
			
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
			
			// Shutdown the service
			executorService.shutdownNow();
			System.out.println("Complete.");
			
			
		}
	}
	
	

}
