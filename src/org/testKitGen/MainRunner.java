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

public class MainRunner {

	private MainRunner() {
	}

	public static void main(String[] args) {
		Options.parse(args);
		MainRunner.start();
	}

	public static void start() {
		System.out.println("\nStarting to generate test make files.\n");
		ModesDictionary.parse();
		MkTreeGen.start();
		Utils.generateFile();
		System.out.println("\nMake files are generated successfully.\n");
		if (!TestTarget.getTestSet().isEmpty()) {
			System.out.println("Warning: cannot find the following tests " + TestTarget.getTestSet().toString().replaceAll("\\s+","") + " in TESTLIST\n");
		}
	}
}