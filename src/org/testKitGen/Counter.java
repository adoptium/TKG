/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package org.testKitGen;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Counter {
	private static Map<String, Integer> count = new HashMap<String, Integer>();
	private static String countmk = Options.getProjectRootDir() + "/TKG/" + Constants.COUNTMK;

	private Counter(Options op) {
	}

	public static void generateFile() {
		FileWriter f;
		try {
			f = new FileWriter(countmk);
			f.write(Constants.HEADERCOMMENTS);

			List<String> targetCountKeys = new ArrayList<>(count.keySet());
			Collections.sort(targetCountKeys);

			f.write("_GROUPTARGET = $(firstword $(MAKECMDGOALS))\n\n");
			f.write("GROUPTARGET = $(patsubst _%,%,$(_GROUPTARGET))\n\n");
			for (String key : targetCountKeys) {
				f.write("ifeq ($(GROUPTARGET)," + key + ")\n");
				f.write("\tTOTALCOUNT := " + count.get(key) + "\n");
				f.write("endif\n\n");
			}

			f.close();

			System.out.println();
			System.out.println("Generated " + countmk);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void add(String key, int value) {
		count.put(key, count.getOrDefault(key, 0) + value);
	}
}