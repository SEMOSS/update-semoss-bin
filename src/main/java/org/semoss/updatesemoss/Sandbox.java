package org.semoss.updatesemoss;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarStyle;

public class Sandbox {

	public static void main0(String[] args) throws UnsupportedEncodingException {
		String test = URLDecoder.decode(
				"DeleteInsightCache(app%3D%5B%22937e52c2-95b2-45a0-8797-46cbd593c1f4%22%5D%2C%20id%3D%5B%22b7238d12-1e1c-4ab4-ab57-8b67a4b904e1%22%5D)%3B",
				"UTF-8");
		System.out.println(test);
	}

	public static void main(String[] arg) throws Exception {
		int fileSize = 112341235;
		int buffer = 2048;
		try (ProgressBar pb = new ProgressBar("Test", fileSize, ProgressBarStyle.ASCII)) { // name, initial max
			for (int i = 0; i < fileSize; i += buffer) {
				Thread.sleep(1);
				pb.stepBy(buffer);
			}
		}
	}

}
