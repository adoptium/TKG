package org.openj9.envInfo;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

public class Utility {

    public static Writer writer;

    public static Writer getWriterObject(int jdkVersion, String SpecInfo, String fileName) {
		try {
			if (SpecInfo.toLowerCase().contains("zos") && (jdkVersion >= 21)) {
				writer = new OutputStreamWriter(new FileOutputStream(fileName, true), Charset.forName("IBM-1047"));
			} else {
				writer = new FileWriter(fileName, true);
			}
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return writer;
	}
}
