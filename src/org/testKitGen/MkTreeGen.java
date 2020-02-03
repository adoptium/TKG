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
import java.util.ArrayList;
import java.util.List;

public class MkTreeGen {

	private MkTreeGen() {
	}

	public static void start() {
		traverse(new ArrayList<String>());
	}

	private static boolean continueTraverse(String currentdir) {
		// If the build list is empty then generate on every project.
		if (Options.getBuildList().isEmpty()) {
			return true;
		}
		// Only generate make files for projects that are specificed in the build list.
		String[] buildListArr = Options.getBuildList().split(",");
		for (String buildPath : buildListArr) {
			buildPath = buildPath.replaceAll("\\+", "/");
			if (currentdir.contains(buildPath) || buildPath.contains(currentdir)) {
				return true;
			}
		}
		return false;
	}

	public static boolean traverse(ArrayList<String> currentdirs) {
		String absolutedir = Options.getProjectRootDir();
		String currentdir = String.join("/", currentdirs);
		if (!currentdirs.isEmpty()) {
			absolutedir = absolutedir + '/' + currentdir;
		}

		if (!continueTraverse(currentdir)) {
			return false;
		}

		File playlistXML = null;
		List<String> subdirs = new ArrayList<String>();

		File directory = new File(absolutedir);
		File[] dir = directory.listFiles();

		for (File entry : dir) {
			File file = new File(absolutedir + '/' + entry.getName());
			if (file.isFile() && entry.getName().equals(Constants.PLAYLIST)) {
				playlistXML = file;
			} else if (file.isDirectory()) {
				currentdirs.add(entry.getName());
				if (traverse(currentdirs)) {
					subdirs.add(entry.getName());
				}
				currentdirs.remove(currentdirs.size() - 1);
			}
		}

		MkGen mg = new MkGen(playlistXML, absolutedir, currentdirs, subdirs);
		boolean rt = mg.start();
		return rt;
	}
}