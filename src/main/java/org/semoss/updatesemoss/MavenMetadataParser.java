package org.semoss.updatesemoss;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.collections.MapUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MavenMetadataParser {
	
	private static final String FS = System.getProperty("file.separator");

	private static final String MAVEN_METADATA = "maven-metadata.xml";
	private static final String VALUE_KEY = "#value";
	private final Map<String, Object> xmlMap;
		
	public MavenMetadataParser(String workingDirectory, String artifactId, String version) throws Exception {
		
		// Retrieve the metadata
		String xmlUrl = UpdateUtil.SONATYPE_PREFIX + artifactId + "/" + version + "/" + MAVEN_METADATA;
		String xmlPath = workingDirectory + FS + MAVEN_METADATA;
		UpdateUtil.downloadFile(xmlUrl, xmlPath);
		File xmlFile = new File(xmlPath);
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.parse(xmlFile);
		
		// Set the xmlMap		
		Map<String, Object> xmlMap = new LinkedHashMap<>();
		populateXmlMap(xmlMap, doc.getDocumentElement().getChildNodes());
		this.xmlMap = xmlMap;
		MapUtils.verbosePrint(System.out, artifactId, xmlMap);
	}
		
	private void populateXmlMap(Map<String, Object> xmlMap, NodeList nodeList) {
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = node.getNodeName();
				if (node.hasChildNodes()) {
					xmlMap.put(nodeName, new HashMap<String, Object>());
					@SuppressWarnings("unchecked")
					Map<String, Object> childMap = (Map<String, Object>) xmlMap.get(nodeName);
					populateXmlMap(childMap, node.getChildNodes());
				}
			} else {
				String nodeValue = node.getNodeValue();
				if (!nodeValue.trim().isEmpty()) {
					xmlMap.put(VALUE_KEY, nodeValue);
				}
			}
		}
	}
	
	public Map<String, Object> getXmlMap() {
		return xmlMap;
	}
	
	@SuppressWarnings("unchecked")
	public String getValue(String... tree) {
		Map<String, Object> subTree = xmlMap;
		for (String key : tree) {
			if (subTree.containsKey(key)) {
				subTree = (Map<String, Object>) subTree.get(key);
			} else {
				return null;
			}
		}
		Object value = subTree.get(VALUE_KEY);
		if (value instanceof String) {
			return subTree.get(VALUE_KEY).toString();
		} else {
			return null;
		}
	}
	
	public static void main(String args[]) throws Exception {
		MavenMetadataParser parser = new MavenMetadataParser("C:\\Users\\tbanach\\Documents\\Workspace\\updatestandalone\\wd", "semoss", "3.3.9.2-SNAPSHOT");
		Map<String, Object> xmlMap = parser.getXmlMap();
		MapUtils.verbosePrint(System.out, "xmlMap", xmlMap);
		System.out.println(parser.getValue("versioning", "snapshot", "buildNumber"));		
	}
	
}