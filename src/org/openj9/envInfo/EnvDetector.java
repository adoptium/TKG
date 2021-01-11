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

	public static void main(String[] args) {
		parseArgs(args);
	}

	private static void parseArgs(String[] args) {
		if (args.length == 0) {
			getJavaInfo();
			getMachineInfo();
		}
		for (int i = 0; i < args.length; i++) {
			String option = args[i].toLowerCase();
			if (option.equals("machineinfo")) {
				getMachineInfo();
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
		int javaVersionInfo = envDetection.getJDKVersion();
		if (SPECInfo == null || javaVersionInfo == -1 || javaImplInfo == null) {
			System.exit(1);
		}
		String SPECvalue = "DETECTED_SPEC=" + SPECInfo + "\n";
		String JDKVERSIONvalue = "DETECTED_JDK_VERSION=" + javaVersionInfo + "\n";
		String JDKIMPLvalue = "DETECTED_JDK_IMPL=" + javaImplInfo + "\n";
		String JDKVENDORvalue = "DETECTED_JDK_VENDOR=" + vendorInfo + "\n";

		/**
		 * autoGenEnv.mk file will be created to store auto detected java info.
		 */
		BufferedWriter output = null;
		try {
			output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("autoGenEnv.mk")));
			output.write("########################################################\n");
			output.write("# This is an auto generated file. Please do NOT modify!\n");
			output.write("########################################################\n");
			output.write(SPECvalue);
			output.write(JDKVERSIONvalue);
			output.write(JDKIMPLvalue);
			output.write(JDKVENDORvalue);
			output.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void getMachineInfo() {
		MachineInfo machineInfo = new MachineInfo();
		machineInfo.getInfo();
		System.out.println("****************************** MACHINE INFO ******************************");
		System.out.println(machineInfo);
		System.out.println("**************************************************************************\n");
	}
}