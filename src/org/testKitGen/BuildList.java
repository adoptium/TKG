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
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.openj9.envInfo.JavaInfo;
import org.openj9.envInfo.Utility;

public class BuildList {
	private Arguments arg;
	private JavaInfo jInfo;
	private Set<String> originalSet = new HashSet<>();
	private Set<String> newSet = new HashSet<>();
	private String buildInfomk;

	public BuildList(Arguments arg) {
		this.arg = arg;
		this.jInfo = new JavaInfo();
		buildInfomk = arg.getProjectRootDir() + "/TKG/" + Constants.BUILDINFOMK;
		initializeSet();
	}

	private void initializeSet() {
		if (!arg.getBuildList().isEmpty()) {
			String[] buildListArr = arg.getBuildList().split(",");
			for (String buildPath : buildListArr) {
				buildPath = buildPath.replaceAll("\\+", "/");
				originalSet.add(buildPath);
			}
		}
	}

	public boolean isRelated(String dir) {
		if (originalSet.isEmpty()) {
			return true;
		}
		for (String buildPath : originalSet) {
			if (dir.equals(buildPath) || dir.equals("") || dir.contains(buildPath + "/") || buildPath.contains(dir + "/")) {
				return true;
			}
		}
		return false;
	}

	public boolean contains(String dir) {
		if (originalSet.isEmpty()) {
			return true;
		}
		for (String buildPath : originalSet) {
			if (dir.equals(buildPath) || dir.contains(buildPath + "/")) {
				return true;
			}
		}
		return false;
	}

	public void add(File playlistXML) {
		Path basePath = Paths.get(arg.getProjectRootDir());
		Path playlistPath = playlistXML.toPath();
		Path relativePath = basePath.relativize(playlistPath);
		newSet.add(relativePath.getParent().toString().replace('\\', '/'));
	}

	private String getStr() {
		Set<String> s = newSet;
		if (arg.getTestTargetName().equals("compile")) {
			s = originalSet;
		} else if (newSet.isEmpty()) {
			newSet.add("NULL");
		} 
		StringBuilder sb = new StringBuilder();
		String sep = "";
		for (String buildPath : s) {
			sb.append(sep).append(buildPath);
			sep =",";
		}
		return sb.toString();
	}

	public void generateList() {
		try (Writer f = Utility.getWriterObject(jInfo.getJDKVersion(), arg.getSpec(), buildInfomk)) {
			f.write(Constants.HEADERCOMMENTS);
			f.write("REFINED_BUILD_LIST := " + getStr() + "\n");
			System.out.println("Generated " + buildInfomk + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void clean() {
		File f = new File(buildInfomk);
		f.delete();
	}
}