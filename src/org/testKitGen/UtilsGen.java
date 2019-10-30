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

public class UtilsGen {
	private static String utilsmk = Options.getProjectRootDir() + "/TKG/" + Constants.UTILSMK;
	private static String dependmk = Options.getProjectRootDir() + "/TKG/" + Constants.DEPENDMK;

	private UtilsGen() {
	}

	public static void start() {
		try {
			genDependMk();
			genUtilsMk();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void genDependMk() throws IOException {
		FileWriter f = new FileWriter(dependmk);

		f.write(Constants.HEADERCOMMENTS);

		List<String> allDisHead = Arrays.asList("", "disabled.", "echo.disabled.");
		for (String eachDisHead : allDisHead) {
			for (String eachLevel : Constants.ALLLEVELS) {
				for (String eachGroup : Constants.ALLGROUPS) {
					String hlgKey = eachDisHead + eachLevel + '.' + eachGroup;
					f.write(hlgKey + ":");
					for (String eachType : Constants.ALLTYPES) {
						String hlgtKey = eachDisHead + eachLevel + '.' + eachGroup + '.' + eachType;
						f.write(" \\\n" + hlgtKey);
					}
					f.write("\n\n.PHONY: " + hlgKey + "\n\n");
				}
			}

			for (String eachGroup : Constants.ALLGROUPS) {
				for (String eachType : Constants.ALLTYPES) {
					String gtKey = eachDisHead + eachGroup + '.' + eachType;
					f.write(gtKey + ":");
					for (String eachLevel : Constants.ALLLEVELS) {
						String lgtKey = eachDisHead + eachLevel + '.' + eachGroup + '.' + eachType;
						f.write(" \\\n" + lgtKey);
					}
					f.write("\n\n.PHONY: " + gtKey + "\n\n");
				}
			}

			for (String eachType : Constants.ALLTYPES) {
				for (String eachLevel : Constants.ALLLEVELS) {
					String ltKey = eachDisHead + eachLevel + '.' + eachType;
					f.write(ltKey + ":");
					for (String eachGroup : Constants.ALLGROUPS) {
						String lgtKey = eachDisHead + eachLevel + '.' + eachGroup + '.' + eachType;
						f.write(" \\\n" + lgtKey);
					}
					f.write("\n\n.PHONY: " + ltKey + "\n\n");
				}
			}

			for (String eachLevel : Constants.ALLLEVELS) {
				String lKey = eachDisHead + eachLevel;
				f.write(lKey + ":");
				for (String eachGroup : Constants.ALLGROUPS) {
					f.write(" \\\n" + eachDisHead + eachLevel + '.' + eachGroup);
				}
				f.write("\n\n.PHONY: " + lKey + "\n\n");
			}

			for (String eachGroup : Constants.ALLGROUPS) {
				String gKey = eachDisHead + eachGroup;
				f.write(gKey + ":");
				for (String eachLevel : Constants.ALLLEVELS) {
					f.write(" \\\n" + eachDisHead + eachLevel + '.' + eachGroup);
				}
				f.write("\n\n.PHONY: " + gKey + "\n\n");
			}

			for (String eachType : Constants.ALLTYPES) {
				String tKey = eachDisHead + eachType;
				f.write(tKey + ":");
				for (String eachLevel : Constants.ALLLEVELS) {
					f.write(" \\\n" + eachDisHead + eachLevel + '.' + eachType);
				}
				f.write("\n\n.PHONY: " + tKey + "\n\n");
			}

			String allKey = eachDisHead + "all";
			f.write(allKey + ":");
			for (String eachLevel : Constants.ALLLEVELS) {
				f.write(" \\\n" + eachDisHead + eachLevel);
			}
			f.write("\n\n.PHONY: " + allKey + "\n\n");
		}

		f.close();

		System.out.println();
		System.out.println("Generated " + dependmk);
	}

	private static void genUtilsMk() throws IOException {
		FileWriter f = new FileWriter(utilsmk);

		f.write(Constants.HEADERCOMMENTS);
		f.write("PLATFORM=\n");
		String plat = ModesDictionary.getPlat(Options.getSpec());
		if (!plat.isEmpty()) {
			f.write("ifeq" + " ($(SPEC)," + Options.getSpec() + ")\n\tPLATFORM=" + plat + "\nendif\n\n");
		}

		f.close();
		System.out.println();
		System.out.println("Generated " + utilsmk);
	}
}