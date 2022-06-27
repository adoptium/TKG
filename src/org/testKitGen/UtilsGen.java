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

public class UtilsGen {
	private Arguments arg;
	private ModesDictionary md;
	private String utilsmk;

	public UtilsGen(Arguments arg, ModesDictionary md) {
		this.arg = arg;
		this.md = md;
		utilsmk = arg.getProjectRootDir() + "/TKG/" + Constants.UTILSMK;
	}

	public void generateFile() {
		try (FileWriter f = new FileWriter(utilsmk)) {
			f.write(Constants.HEADERCOMMENTS);
			f.write("TOTALCOUNT := " + TestInfo.numOfTests() + "\n");
			f.write("PLATFORM=\n");
			String plat = md.getPlat();
			if (!plat.isEmpty()) {
				f.write("ifeq" + " ($(SPEC)," + arg.getSpec() + ")\n\tPLATFORM=" + plat + "\nendif\n\n");
			}
			System.out.println("Generated " + utilsmk + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void clean() {
		File f = new File(utilsmk);
		f.delete();
	}
}