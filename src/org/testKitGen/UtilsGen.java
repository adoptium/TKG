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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class UtilsGen {
	private Arguments arg;
	private ModesDictionary md;
	private String utilsMk;
	private String rerunMk;
	private List<String> ignoreOnRerunList;

	public UtilsGen(Arguments arg, ModesDictionary md, List<String> ignoreOnRerunList) {
		this.arg = arg;
		this.md = md;
		this.utilsMk = arg.getProjectRootDir() + "/TKG/" + Constants.UTILSMK;
		this.rerunMk = arg.getProjectRootDir() + "/TKG/" + Constants.RERUNMK;
		this.ignoreOnRerunList = ignoreOnRerunList;
	}

	public void generateFiles() {
		generateUtil();
		generateRerun();
	}

	private void generateUtil() {
		try (FileWriter f = new FileWriter(utilsMk)) {
			f.write(Constants.HEADERCOMMENTS);
			f.write("TOTALCOUNT := " + TestInfo.numOfTests() + "\n");
			f.write("PLATFORM=\n");
			String plat = md.getPlat();
			if (!plat.isEmpty()) {
				f.write("ifeq" + " ($(SPEC)," + arg.getSpec() + ")\n\tPLATFORM=" + plat + "\nendif\n\n");
			}
			System.out.println("Generated " + utilsMk + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void generateRerun() {
		try (FileWriter f = new FileWriter(rerunMk)) {
			f.write(Constants.HEADERCOMMENTS);
			String ignoreOnRerunStr = String.join(",", ignoreOnRerunList);
			f.write("IGNORE_ON_RERUN=");
			if (ignoreOnRerunList != null && !ignoreOnRerunList.isEmpty()) {
				f.write(ignoreOnRerunStr);
			}
			System.out.println("Generated " + rerunMk + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void clean() {
		File f = new File(utilsMk);
		f.delete();
		f = new File(rerunMk);
		f.delete();
	}
}