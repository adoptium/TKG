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

public class DirectoryWalker {
	private Arguments arg;
	private DirectoryVisitor dv;
	private ArrayList<String> dirList;

	public DirectoryWalker(Arguments arg, DirectoryVisitor dv) {
		this.arg = arg;
		this.dv = dv;
		dirList = new ArrayList<String>();
	}

	private boolean continueTraverse(String currentDir) {
		// If the build list is empty then generate on every project.
		if (arg.getBuildList().isEmpty()) {
			return true;
		}
		// Only generate make files for projects that are specified in the build list.
		String[] buildListArr = arg.getBuildList().split(",");
		for (String buildPath : buildListArr) {
			buildPath = buildPath.replaceAll("\\+", "/");
			if (currentDir.equals(buildPath) || currentDir.equals("") || currentDir.contains(buildPath + "/") || buildPath.contains(currentDir + "/")) {
				return true;
			}
		}
		return false;
	}

	public boolean traverse() {
		String absoluteDir = arg.getProjectRootDir();
		String currentDir = String.join("/", dirList);
		if (!dirList.isEmpty()) {
			absoluteDir = absoluteDir + '/' + currentDir;
		}

		if (!continueTraverse(currentDir)) {
			return false;
		}

		File playlistXML = null;

		File directory = new File(absoluteDir);
		File[] dir = directory.listFiles();
		List<String> subDirsWithTest = new ArrayList<String>();
		for (File entry : dir) {
			File file = new File(absoluteDir + '/' + entry.getName());
			if (file.isFile() && entry.getName().equals(Constants.PLAYLIST)) {
				playlistXML = file;
			} else if (file.isDirectory()) {
				dirList.add(entry.getName());
				if (traverse()) {
					subDirsWithTest.add(entry.getName());
				}
				dirList.remove(dirList.size() - 1);
			}
		}

		boolean rt = dv.visit(playlistXML, absoluteDir, dirList, subDirsWithTest);
		return rt;
	}
}