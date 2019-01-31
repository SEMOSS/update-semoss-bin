package org.semoss.updatestandalone;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class Sandbox {

	public static void main(String[] args) throws UnsupportedEncodingException {
		String test = URLDecoder.decode("DeleteInsightCache(app%3D%5B%22937e52c2-95b2-45a0-8797-46cbd593c1f4%22%5D%2C%20id%3D%5B%22b7238d12-1e1c-4ab4-ab57-8b67a4b904e1%22%5D)%3B","UTF-8");
		System.out.println(test);
	}
	
}
