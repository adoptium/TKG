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
import java.util.Arrays;
import java.util.List;

public class Utils {
	private static String utilsmk = Options.getProjectRootDir() + "/TKG/" + Constants.UTILSMK;
	private static int count = 0;

	private Utils() {
	}

	public static void generateFile() {
		try {
			FileWriter f = new FileWriter(utilsmk);

			f.write(Constants.HEADERCOMMENTS);
			f.write("TOTALCOUNT := " + count + "\n");
			f.write("PLATFORM=\n");
			String plat = ModesDictionary.getPlat(Options.getSpec());
			if (!plat.isEmpty()) {
				f.write("ifeq" + " ($(SPEC)," + Options.getSpec() + ")\n\tPLATFORM=" + plat + "\nendif\n\n");
			}

			f.close();
			System.out.println();
			System.out.println("Generated " + utilsmk);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void count() {
		count++;
	}
}