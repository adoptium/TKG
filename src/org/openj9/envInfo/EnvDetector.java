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

package org.openj9.envInfo;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.BufferedWriter;

public class EnvDetector {
	static boolean isMachineInfo = false;
	static boolean isJavaInfo = false;
	static MachineInfo machineInfo = null;

	public static void main(String[] args) {
		parseArgs(args);
	}

	private static void parseArgs(String[] args) {
		machineInfo = new MachineInfo();
		machineInfo.checkInfo();
		if (args.length == 0) {
			printMachineInfo();
			getJavaInfo();
		}
		for (int i = 0; i < args.length; i++) {
			String option = args[i].toLowerCase();
			if (option.equals("machineinfo")) {
				printMachineInfo();
			} else if (option.equals("javainfo")) {
				getJavaInfo();
			}
		}
	}

	/*
	 * getJavaInfo() is used for AUTO_DETECT
	 */
	private static void getJavaInfo() {

		JavaInfo envDetection = new JavaInfo();
		String javaImplInfo = envDetection.getJDKImpl();
		String vendorInfo = envDetection.getJDKVendor();
		String SPECInfo = envDetection.getSPEC(javaImplInfo);
		String javaVersion = envDetection.getJavaVersion();
		String testFlag = envDetection.getTestFlag();
		int javaVersionInfo = envDetection.getJDKVersion();
		String releaseInfo = envDetection.getReleaseInfo();
		if (SPECInfo == null || javaVersionInfo == -1 || javaImplInfo == null) {
			System.exit(1);
		}
		String MICRO_ARCH = "";
		if (machineInfo.getInfoMap().containsKey("microArch")) {
			MICRO_ARCH = "DETECTED_MICRO_ARCH=" + machineInfo.getInfoMap().get("microArch").output + "\n";
		}
		String OS_LABEL = "";
		if (machineInfo.getInfoMap().containsKey("osLabel")) {
			OS_LABEL = "DETECTED_OS_LABEL=" + machineInfo.getInfoMap().get("osLabel").output + "\n";
		}
		String SPEC = "DETECTED_SPEC=" + SPECInfo + "\n";
		String JDK_VERSION = "DETECTED_JDK_VERSION=" + javaVersionInfo + "\n";
		String JDK_IMPL = "DETECTED_JDK_IMPL=" + javaImplInfo + "\n";
		String JDK_VENDOR = "DETECTED_JDK_VENDOR=" + vendorInfo + "\n";
		String JAVA_VERSION = "DETECTED_JAVA_VERSION=" + javaVersion + "\n";
		String RELEASE_INFO = "DETECTED_RELEASE_INFO=" + releaseInfo + "\n";
		String TEST_FLAG = "DETECTED_TEST_FLAG=" + testFlag + "\n";

		/**
		 * autoGenEnv.mk file will be created to store auto detected java info.
		 */
		BufferedWriter output = null;
		try {
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("autoGenEnv.mk")));
			output.write("########################################################\n");
			output.write("# This is an auto generated file. Please do NOT modify!\n");
			output.write("########################################################\n");
			output.write(SPEC);
			output.write(MICRO_ARCH);
			output.write(OS_LABEL);
			output.write(JDK_VERSION);
			output.write(JDK_IMPL);
			output.write(JDK_VENDOR);
			output.write(TEST_FLAG);
			output.close();
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("AQACert.log")));
			output.write(JAVA_VERSION);
			output.write(RELEASE_INFO);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void printMachineInfo() {
		System.out.println("****************************** MACHINE INFO ******************************");
		System.out.println(machineInfo);
		System.out.println("**************************************************************************\n");
	}
}