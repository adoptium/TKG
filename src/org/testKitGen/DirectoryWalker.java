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
	private BuildList bl;
	private ArrayList<String> dirList;
	private List<String> ignoreOnRerunList;

	public DirectoryWalker(Arguments arg, DirectoryVisitor dv, BuildList bl, List<String> ignoreOnRerunList) {
		this.arg = arg;
		this.dv = dv;
		this.bl = bl;
		this.ignoreOnRerunList = ignoreOnRerunList;
		dirList = new ArrayList<String>();
	}

	public boolean traverse() {
		String absoluteDir = arg.getProjectRootDir();
		String currentDir = String.join("/", dirList);
		if (!dirList.isEmpty()) {
			absoluteDir = absoluteDir + '/' + currentDir;
		}

		if (!bl.isRelated(currentDir)) {
			return false;
		}

		File playlistXML = null;

		File directory = new File(absoluteDir);
		File[] dir = directory.listFiles();
		List<String> subDirsWithTest = new ArrayList<String>();
		for (File entry : dir) {
			File file = new File(absoluteDir + '/' + entry.getName());
			if (bl.contains(currentDir) && file.isFile() && entry.getName().equals(Constants.PLAYLIST)) {
				playlistXML = file;
			} else if (file.isDirectory()) {
				dirList.add(entry.getName());
				if (traverse()) {
					subDirsWithTest.add(entry.getName());
				}
				dirList.remove(dirList.size() - 1);
			}
		}

		boolean rt = dv.visit(playlistXML, absoluteDir, dirList, subDirsWithTest, ignoreOnRerunList);
		return rt;
	}
}