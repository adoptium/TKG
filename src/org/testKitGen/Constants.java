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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

public final class Constants {
	public static final String PLAYLIST = "playlist.xml";
	public static final String RESOURCE = "resources";
	public static final String MODESXML = Constants.RESOURCE + "/modes.xml";
	public static final String OTTAWACSV = Constants.RESOURCE + "/ottawa.csv";
	public static final String PLAYLISTXSD = Constants.RESOURCE + "/playlist.xsd";
	public static final String BUILDPLAT_JSON = Constants.RESOURCE + "/buildPlatformMap.properties";
	public static final String TRSSCACHE_DIR = Constants.RESOURCE + "/TRSS";
	public static final String TESTMK = "autoGen.mk";
	public static final String UTILSMK = "utils.mk";
	public static final String SETTINGSMK = "settings.mk";
	public static final String PARALLELMK = "parallelList.mk";
	public static final String BUILDINFOMK = "buildInfo.mk";
	public static final String HEADERCOMMENTS = "########################################################\n"
			+ "# This is an auto generated file. Please do NOT modify!\n"
			+ "########################################################\n\n";
	public static final List<String> ALLGROUPS = Arrays.asList("functional", "openjdk", "external", "perf", "jck",
			"system");
	public static final List<String> ALLIMPLS = Arrays.asList("openj9", "ibm", "hotspot", "sap");
	public static final List<String> ALLLEVELS = Arrays.asList("sanity", "extended", "special", "dev");
	public static final List<String> ALLTYPES = Arrays.asList("regular", "native");
	public static final Set<String> INGORESPECS = Arrays
			.asList("linux_x86-32_hrt", "linux_x86-64_cmprssptrs_gcnext", "linux_ppc_purec", "linux_ppc-64_purec",
					"linux_ppc-64_cmprssptrs_purec", "linux_x86-64_cmprssptrs_cloud")
			.stream().collect(Collectors.toSet());
	public static final String TRSS_URL = "https://trss.adoptopenjdk.net";
	private Constants() {
	}
}