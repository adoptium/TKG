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

public class TestGenVisitor implements DirectoryVisitor {
	private Arguments arg;
	private ModesDictionary md;
	private TestTarget tt;

	public TestGenVisitor(Arguments arg, ModesDictionary md, TestTarget tt) {
		this.arg = arg;
		this.md = md;
		this.tt = tt;
	}

	@Override
	public boolean visit(File playlistXML, String absoluteDir, List<String> dirList, List<String> subDirs) {
		PlaylistInfoParser parser = new PlaylistInfoParser(arg, md, tt, playlistXML);
		PlaylistInfo pli = parser.parse();
		boolean testFound = !subDirs.isEmpty() || pli.containsTest();
		String makeFile = absoluteDir + "/" + Constants.TESTMK;
		File file = new File(makeFile); 
		file.delete();
		if (testFound) {
			MkGen mg = new MkGen(arg, tt, pli, makeFile, dirList, subDirs);
			mg.start();
			return true;
		}
		return false;
	}
}