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

public class Arguments {
	enum Mode { 
		GEN_TESTS, GEN_PARALLEL_LIST;
	}
	private static Arguments instance;
	private Mode mode = Mode.GEN_TESTS;
	private String spec = "";
	private String jdkVersion = "";
	private String impl = "";
	private String projectRootDir = System.getProperty("user.dir") + "/..";
	private String buildList = "";
	private String iterations = "";
	private String testFlag = "";
	private Integer numOfMachines = null;
	private Integer testTime = null;
	private String TRSSURL = "";

	private static final String usage = "Usage:\n"
			+ "    java TestKitGen --mode=[tests|parallelList] --spec=[linux_x86-64] --jdkVersion=[8|9|...] --impl=[openj9|ibm|hotspot|sap] [options]\n\n"
			+ "Options:\n" + "    --spec=<spec>           Spec that the build will run on\n"
			+ "    --mode=<string>           Specify running mode, available modes are tests or parallelList\n"
			+ "                              tests is to generate test make files\n"
			+ "                              parallelList is generate parallel list file\n"
			+ "                              Defaults to tests\n"
			+ "    --jdkVersion=<version>    JDK version that the build will run on, e.g. 8, 9, 10, etc.\n"
			+ "    --impl=<implementation>   Java implementation, e.g. openj9, ibm, hotspot, sap\n"
			+ "    --projectRootDir=<path>   Root path for searching playlist.xml\n"
			+ "                              Defaults to the parent folder of TKG \n"
			+ "    --buildList=<paths>       Comma separated project paths (relative to projectRootDir) to search for playlist.xml\n"
			+ "                              Defaults to projectRootDir\n"
			+ "    --iterations=<number>     Repeatedly generate test command based on iteration number\n"
			+ "                              Defaults to 1\n"
			+ "    --testFlag=<string>       Comma separated string to specify different test flags\n"
			+ "                              Defaults to \"\"\n"
			+ "    --testTarget=<string>     Test target to execute\n"
			+ "                              Defaults to all\n"
			+ "    --numOfMachines=<number>  Specify number of machines for mode parallelList \n"
			+ "                              Defaults to 1\n"
			+ "    --testTime=<number>       Specify expected length of test running time (minutes) on each machines for mode parallelList, this option will be suppressed if numOfMachines is given\n"
			+ "                              If testTime and numOfMachines are not provided, default numOfMachines will be used\n"
			+ "    --TRSSURL=<serverURL>     Specify the TRSS server URL for mode parallelList\n"
			+ "                              Defaults to " + Constants.TRSS_URL + "\n";
			

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

	public String getJdkVersion() {
		return jdkVersion;
	}

	public String getImpl() {
		return impl;
	}

	public String getProjectRootDir() {
		return projectRootDir;
	}

	public String getBuildList() {
		return buildList;
	}

	public String getIterations() {
		return iterations;
	}

	public String getTestFlag() {
		return testFlag;
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

	public void parse(String[] args) {
		String testTarget = null;
		String testList = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			String arglc = arg.toLowerCase();
			if (arglc.startsWith("--mode=")) {
				String modeStr = arglc.substring(arg.indexOf("=") + 1);
				if (modeStr.equals("tests") || modeStr.isEmpty()) {
					mode = Mode.GEN_TESTS;
				} else if (modeStr.equals("parallellist")) {
					mode = Mode.GEN_PARALLEL_LIST;
				} else {
					System.err.println("Invalid mode: " + modeStr);
					System.err.println(usage);
					System.exit(1);
				}
			} else if (arglc.startsWith("--spec=")) {
				spec = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--jdkversion=")) {
				jdkVersion = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--impl=")) {
				impl = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--projectrootdir=")) {
				// projectRootDir is case sensitive
				projectRootDir = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--buildlist=")) {
				// buildList is case sensitive
				buildList = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--iterations=")) {
				iterations = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--testflag=")) {
				testFlag = arglc.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--testlist=")) {
				// test list is case sensitive
				testList = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--testtarget=")) {
				// test Target is case sensitive
				testTarget = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--trssurl=")) {
				// TRSSURL is case sensitive
				TRSSURL = arg.substring(arg.indexOf("=") + 1);
			} else if (arglc.startsWith("--numofmachines")) {
				String numOfMachinesStr = arg.substring(arg.indexOf("=") + 1);
				if (!numOfMachinesStr.isEmpty()) {
					numOfMachines = Integer.valueOf(numOfMachinesStr);
					if (numOfMachines <= 0) {
						System.err.println("Invalid option: " + arg);
						System.err.println("Num of machines need to be greater than 0");
						System.exit(1);
					}
				}
			} else if (arglc.startsWith("--testtime")) {
				String testTimeStr = arg.substring(arg.indexOf("=") + 1);
				if (!testTimeStr.isEmpty()) {
					testTime = Integer.valueOf(testTimeStr);
					if (testTime <= 0) {
						System.err.println("Invalid option: " + arg);
						System.err.println("Test time needs to be greater than 0");
						System.exit(1);
					}
				}
			} else {
				System.err.println("Invalid option: " + arg);
				System.err.println(usage);
				System.exit(1);
			}
		}
		TestTarget.parse(testTarget, testList);
	}
}