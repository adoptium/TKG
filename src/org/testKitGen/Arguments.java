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

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Arrays;
import java.util.List;
public class Arguments {
	enum Mode { 
		GEN_TESTS, GEN_PARALLEL_LIST, GEN_BUILD_LIST, CLEAN;
	}
	private static Arguments instance;
	private Mode mode = Mode.GEN_TESTS;
	private String spec = "";
	private String microArch = "";
	private String osLabel = "";
	private String plat = "";
	private String jdkVersion = "";
	private String impl = "";
	private String buildImpl = "";
	private String vendor = "";
	private String projectRootDir = System.getProperty("user.dir") + "/..";
	private String buildList = "";
	private Integer iterations = null;
	private Integer aotIterations = null;
	private String testFlag = "";
	private Integer numOfMachines = null;
	private Integer testTime = null;
	private String TRSSURL = "";
	private String testTargetName = null;
	private String testList = null;

	private static final String usage = "Usage:\n"
			+ "    java TestKitGen --mode=[tests|parallelList] --spec=[linux_x86-64] --jdkVersion=[8|9|...] --impl=[openj9|ibm|hotspot|sap] [options]\n\n"
			+ "Options:\n" + "    --spec=<spec>           Spec that the build will run on\n"
			+ "    --mode=<string>           Specify running mode, available modes are tests, parallelList, buildList or clean\n"
			+ "                              tests is to generate test make files\n"
			+ "                              parallelList is to generate parallel list file\n"
			+ "                              buildList is to generate build list file\n"
			+ "                              clean is to remove generated files\n"
			+ "                              Defaults to tests\n"
			+ "    --jdkVersion=<version>    JDK version that the build will run on, e.g. 8, 9, 10, etc.\n"
			+ "    --impl=<implementation>   Java implementation, e.g. openj9, ibm, hotspot, sap\n"
			+ "    --vendor=<jdk vendor>     Java vendor information, e.g. adoptopenjdk, ibm\n"
			+ "    --projectRootDir=<path>   Root path for searching playlist.xml\n"
			+ "                              Defaults to the parent folder of TKG \n"
			+ "    --buildList=<paths>       Comma separated project paths (relative to projectRootDir) to search for playlist.xml\n"
			+ "                              Defaults to projectRootDir\n"
			+ "    --iterations=<number>     Repeatedly generate test command based on iterations number\n"
			+ "                              Defaults to 1\n"
			+ "    --aotIterations=<number>  Repeatedly generate aot test command based on aotIterations number.\n"
			+ "                              When both iterations and aotIterations is set, the larger parameter will be applied on AOT applicable tests. AOT explicit tests will be repeated based on the iterations parameter.\n"
			+ "                              Defaults to false\n"
			+ "    --testFlag=<string>       Comma separated string to specify different test flags\n"
			+ "                              Defaults to \"\"\n"
			+ "    --testTarget=<string>     Test target to execute\n"
			+ "                              Defaults to all\n"
			+ "    --numOfMachines=<number>  Specify number of machines for mode parallelList \n"
			+ "    --testTime=<number>       Specify expected length of test running time (minutes) on each machines for mode parallelList, this option will be suppressed if numOfMachines is given\n"
			+ "                              If testTime and numOfMachines are not provided, default numOfMachines will be used\n"
			+ "    --TRSSURL=<serverURL>     Specify the TRSS server URL for mode parallelList\n"
			+ "                              Defaults to " + Constants.TRSS_URL + "\n"
			+ "    --microArch=<microArch>   Specify micro-architecture\n"
			+ "                              Defaults to  \"\"\n"
			+ "    --osLabel=<osLabel>       Specify OS Label\n"
			+ "                              Defaults to  \"\"\n";
			

	private Arguments() {
	}

	public static Arguments getInstance() {
		if (instance == null) {
			instance = new Arguments();
		}
		return instance;
	}

	public Mode getMode() {
		return mode;
	}

	public String getSpec() {
		return spec;
	}

	public String getMicroArch() {
		return microArch;
	}

	public String getOsLabel() {
		return osLabel;
	}

	public String getPlat() {
		return plat;
	}

	public String getJdkVersion() {
		return jdkVersion;
	}

	public String getImpl() {
		return impl;
	}

	public String getBuildImpl() {
		return buildImpl;
	}

	public String getVendor() {
		return vendor;
	}

	public String getProjectRootDir() {
		return projectRootDir;
	}

	public String getBuildList() {
		return buildList;
	}

	public Integer getIterations() {
		return iterations;
	}

	public Integer getAotIterations() {
		if (!getTestFlag().contains("aot")) {
			return 1;
		}
		return aotIterations;
	}

	public List<String> getTestFlag() {
		return Arrays.asList(testFlag.split(","));
	}

	public Integer getNumOfMachines() {
		return numOfMachines;
	}

	public Integer getTestTime() {
		return testTime;
	}

	public String getTRSSURL() {
		return TRSSURL;
	}

	public String getTestTargetName() {
		return testTargetName;
	}

	public String getTestList() {
		return testList;
	}

	private Integer parsePositiveArgOrDefault(String arg, Integer defaultValue) {
		String str = arg.substring(arg.indexOf("=") + 1);
		Integer result = defaultValue;
		if (!str.isEmpty()) {
			result = Integer.valueOf(str);
			if (result <= 0) {
				System.err.println("Invalid option: " + arg);
				System.err.println("The value needs to be greater than 0");
				System.exit(1);
			}
		}
		return result;
	}

	private String spec2Plat(String spec) {
		String plat = "";
		try (FileReader platReader = new FileReader(Constants.BUILDPLAT_JSON)) {
			Properties platProp = new Properties();
			platProp.load(platReader);
			plat = platProp.getProperty(spec);
			if (plat == null) {
				System.err.println("Error: Please update file " + Constants.BUILDPLAT_JSON + "! Add entry for " + spec + ".");
				System.exit(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return plat;
	}

	public void parse(String[] args) {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			String arglc = arg.toLowerCase();
			if (arglc.startsWith("--mode=")) {
				String modeStr = arglc.substring(arg.indexOf("=") + 1);
				if (modeStr.equals("tests") || modeStr.isEmpty()) {
					mode = Mode.GEN_TESTS;
				} else if (modeStr.equals("parallellist")) {
					mode = Mode.GEN_PARALLEL_LIST;
				} else if (modeStr.equals("buildlist")) {
					mode = Mode.GEN_BUILD_LIST;
				} else if (modeStr.equals("clean")) {
					mode = Mode.CLEAN;
				} else {
					System.err.println("Invalid mode: " + modeStr);
					System.err.println(usage);
					System.exit(1);
				}
			} else if (arglc.startsWith("--spec=")) {
				spec = arglc.substring(arg.indexOf("=") + 1);
				plat = spec2Plat(spec);
			} else if (arglc.startsWith("--microarch=")) {
				microArch = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--oslabel=")) {
				osLabel = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--jdkversion=")) {
				jdkVersion = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--impl=")) {
				impl = arglc.substring(arg.indexOf("=") + 1);
				if (impl.equals("openj9")) {
					buildImpl = "j9";
				} else if (impl.equals("hotspot")) {
					buildImpl =  "hs";
				} else {
					buildImpl = impl;
				}
			} else if (arglc.startsWith("--vendor=")) {
				vendor = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--projectrootdir=")) {
				// projectRootDir is case sensitive
				projectRootDir = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--buildlist=")) {
				// buildList is case sensitive
				buildList = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--iterations=")) {
				iterations = parsePositiveArgOrDefault(arg, 1);
			} else if  (arglc.startsWith("--aotiterations=")) {
				aotIterations = parsePositiveArgOrDefault(arg, 1);
			} else if (arglc.startsWith("--testflag=")) {
				testFlag = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--testlist=")) {
				// test list is case sensitive
				testList = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--testtarget=")) {
				// test Target is case sensitive
				testTargetName = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--trssurl=")) {
				// TRSSURL is case sensitive
				TRSSURL = arg.substring(arg.indexOf("=") + 1);
				TRSSURL = TRSSURL.replaceAll("/$", "");
			} else if (arglc.startsWith("--numofmachines")) {
				numOfMachines = parsePositiveArgOrDefault(arg, null);
			} else if (arglc.startsWith("--testtime")) {
				testTime = parsePositiveArgOrDefault(arg, null);
			} else {
				System.err.println("Invalid option: " + arg);
				System.err.println(usage);
				System.exit(1);
			}
		}
	}
}