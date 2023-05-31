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
import java.util.List;

public class BuildListGenVisitor implements DirectoryVisitor {
	private Arguments arg;
	private ModesDictionary md;
	private TestTarget tt;
	private BuildList bl;

	public BuildListGenVisitor(Arguments arg, ModesDictionary md, TestTarget tt, BuildList bl) {
		this.arg = arg;
		this.md = md;
		this.tt = tt;
		this.bl = bl;
	}

	@Override
	public boolean visit(File playlistXML, String absoluteDir, List<String> dirList, List<String> subDirs, List<String> ignoreOnRerunList) {
		PlaylistInfoParser parser = new PlaylistInfoParser(arg, md, tt, playlistXML, bl);
		parser.parse();
		return true;
	}
}