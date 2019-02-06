package org.semoss.updatesemoss;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.TreeSet;

public class Props {

	public static final String PROPERTY_FILE_PATH_KEY = "property.file.path";
	
	// Assumes alphabetical order when saving to file
	private static Properties props = new Properties() {
		private static final long serialVersionUID = 1L;

		@Override
	    public synchronized Enumeration<Object> keys() {
	        return Collections.enumeration(new TreeSet<Object>(super.keySet()));
	    }
	};

	private final String propertyFilePath;
	
	// Singleton properties class
	// This needs the property.file.path system property defined
	private Props() {
		propertyFilePath = System.getProperty(PROPERTY_FILE_PATH_KEY); 
		
		try (InputStream in = new FileInputStream(propertyFilePath)) {
			props.load(in);
		} catch (IOException e) {
			String noPropsMessage = "Unable to read properties from " + propertyFilePath + ". Check that the "
					+ propertyFilePath + " file exists.";

			// No props, no point in running this program
			throw new IllegalArgumentException(noPropsMessage, e);
		}
	}

    private static class LazyHolder {
    	
    	private LazyHolder() {
    		throw new IllegalStateException("Static class");
    	}
    	
        private static final Props INSTANCE = new Props();
    }
	
	public static Props getInstance() {
		return LazyHolder.INSTANCE;
	}

	public String getProperty(String propertyName) {
		return getProperty(propertyName, "");
	}
	
	public String getProperty(String propertyName, String defaultValue) {
		return props.getProperty(propertyName, defaultValue);
	}
	
	public void setProperty(String propertyName, String propertyValue) {
		props.put(propertyName, propertyValue);
	}
	
	public void removeProperty(String propertyName) {
		props.remove(propertyName);
	}
	
	public void flushPropertiesToFile() throws IOException {
		try (FileOutputStream out = new FileOutputStream(propertyFilePath)) {
			props.store(out, null);
		}
	}
	
}
