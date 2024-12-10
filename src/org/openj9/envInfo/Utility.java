package org.openj9.envInfo;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

public class Utility {

    public static Writer writer;

    public static Writer getWriterObject(String jdkVersion, String SpecInfo, String filetype) {
		try {
			if (SpecInfo.toLowerCase().contains("zos") && (jdkVersion.matches("[2-9][0-9]"))) {
				writer = new OutputStreamWriter(new FileOutputStream(filetype, true), Charset.forName("IBM-1047"));
			} else {
				writer = new FileWriter(filetype, true);
			}
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return writer;
	}
}
