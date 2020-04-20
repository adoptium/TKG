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

public class Options {
	enum Mode { 
		GEN_TESTS, GEN_PARALLEL_LIST;
	}
	private static Mode mode = Mode.GEN_TESTS;
	private static String spec = "";
	private static String jdkVersion = "";
	private static String impl = "";
	private static String projectRootDir = System.getProperty("user.dir") + "/..";
	private static String buildList = "";
	private static String iterations = "";
	private static String testFlag = "";
	private static Integer numOfMachine = null;
	private static Integer testTime = null;

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
			+ "    --numOfMachine=<number>   Specify number of machines for mode parallelList \n"
			+ "                              Defaults to 1\n"
			+ "    --testTime=<number>       Specify expected length of test running time (minutes) on each machines for mode parallelList, this option will be suppressed if numOfMachine is given\n"
			+ "                              If testTime and numOfMachine are not provided, default numOfMachine will be used\n";
			

	private Options() {
	}

	public static Mode getMode() {
		return mode;
	}

	public static String getSpec() {
		return spec;
	}

	public static String getJdkVersion() {
		return jdkVersion;
	}

	public static String getImpl() {
		return impl;
	}

	public static String getProjectRootDir() {
		return projectRootDir;
	}

	public static String getBuildList() {
		return buildList;
	}

	public static String getIterations() {
		return iterations;
	}

	public static String getTestFlag() {
		return testFlag;
	}

	public static Integer getNumOfMachine() {
		return numOfMachine;
	}

	public static Integer getTestTime() {
		return testTime;
	}

	public static void parse(String[] args) {
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
			} else if (arglc.startsWith("--numofmachine")) {
				String numOfMachineStr = arg.substring(arg.indexOf("=") + 1);
				if (!numOfMachineStr.isEmpty()) {
					numOfMachine = Integer.valueOf(numOfMachineStr);
					if (numOfMachine <= 0) {
						System.err.println("Invalid option: " + arg);
						System.err.println("Num of machine needs to be bigger than 0");
						System.exit(1);
					}
				}
			} else if (arglc.startsWith("--testtime")) {
				String testTimeStr = arg.substring(arg.indexOf("=") + 1);
				if (!testTimeStr.isEmpty()) {
					testTime = Integer.valueOf(testTimeStr);
					if (testTime <= 0) {
						System.err.println("Invalid option: " + arg);
						System.err.println("Test time needs to be bigger than 0");
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