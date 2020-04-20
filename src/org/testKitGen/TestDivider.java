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
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestDivider {
	private static List<String> regularTests = new ArrayList<String>();
	private static List<String> effortlessTests = new ArrayList<String>();	// tests almost take no time


	
	private static String parallelmk = Options.getProjectRootDir() + "/TKG/" + Constants.PARALLELMK;
	private static List<List<String>> parallelLists = new ArrayList<List<String>>();
	private static int avgTestTime = 2;
	private TestDivider() {
	}

	public static void addTests(File playlistXML) {
		PlaylistInfo pli = new PlaylistInfo(playlistXML);
		if (pli.parseInfo()) {
			for (TestInfo testInfo : pli.getTestInfoList()) {
				for (Variation var : testInfo.getVars()) {
					String testName = var.getSubTestName();
					if (TestTarget.isSingleTest() || TestTarget.isList()) {
						if (var.isValid()) {
							regularTests.add(testName);
						} else {
							effortlessTests.add(testName);
						}
					} else if (TestTarget.isCategory()) {
						if (!var.isValid()) {
							effortlessTests.add(testName);
						} else {
							if (TestTarget.isDisabled() == testInfo.isDisabled()) {
								regularTests.add(testName);
							} else {
								effortlessTests.add(testName);
							}
						}
					}
				}
			}
		}
	}

	private static void divideOnTestTime(int testTime) {
		int testEachList = testTime / avgTestTime;
		/* If a single test time is bigger than allowed time on each machine, run 1 test per machine. */
		if (testEachList == 0) {
			testEachList = 1;
		}
		/* Populate regular tests first and append the effortless along to the regular test lists. */
		populateParallelLists(regularTests, testEachList, 0);

		/* If no regular test, no need to divide the effortless tests. */
		int numOfMachine = parallelLists.size() != 0 ? parallelLists.size() : 1;
		testEachList = effortlessTests.size() / numOfMachine;
		int remainder = effortlessTests.size() % numOfMachine;
		populateParallelLists(effortlessTests, testEachList, remainder);
	}

	private static void divideOnMachineNum(int numOfMachine) {
		int testEachList = regularTests.size() / numOfMachine;
		int remainder = regularTests.size() % numOfMachine;
		populateParallelLists(regularTests, testEachList, remainder);

		testEachList = effortlessTests.size() / numOfMachine;
		remainder = effortlessTests.size() % numOfMachine;
		populateParallelLists(effortlessTests, testEachList, remainder);
	}


	public static void generateLists() {
		if (Options.getNumOfMachine() == null) {
			if (Options.getTestTime() == null) {
				divideOnMachineNum(1);
			} else {
				divideOnTestTime(Options.getTestTime());
			}
		} else {
			divideOnMachineNum(Options.getNumOfMachine());
		}

		try {
			FileWriter f = new FileWriter(parallelmk);
			f.write(Constants.HEADERCOMMENTS);
			f.write("NUM_LIST=" + parallelLists.size() + "\n\n");
			for (int i = 0; i < parallelLists.size(); i++) {
				f.write("testList_" + i + ":\n");
				f.write("\t$(MAKE) _testList TESTLIST=");
				for (int j = 0; j < parallelLists.get(i).size(); j++) {
					f.write(parallelLists.get(i).get(j));
					if (j != parallelLists.get(i).size() - 1) {
						f.write(",");
					}
				}
				f.write("\n\n");
			}
			f.close();
			System.out.println();
			System.out.println("Generated " + parallelmk + "\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		int totalNum = regularTests.size() + effortlessTests.size();
		System.out.println("Test target (" + totalNum + " tests) is splitted into " + parallelLists.size() + " lists as follows:\n");

		for (int i = 0; i < parallelLists.size(); i++) {
			System.out.println("-------------------------------------testList_" + i + "-------------------------------------");
			System.out.println("number of tests in the list: " + parallelLists.get(i).size());
			System.out.print("TESTLIST=");
			for (int j = 0; j < parallelLists.get(i).size(); j++) {
				System.out.print(parallelLists.get(i).get(j));
				if (j != parallelLists.get(i).size() - 1) {
					System.out.print(",");
				}
			}
			System.out.println("\n------------------------------------------------------------------------------------\n");
		}
	}

	private static void populateParallelLists(List<String> tests, int testEachList, int remainder) {
		/* it's better to have adjacent tests stay archived together */
		int listIndex = 0;
		int start = 0;
		while (start < tests.size()) {
			int end = start + testEachList;
			if (remainder > 0) {
				end += 1;
				remainder--;
			}
			if (listIndex >= parallelLists.size()) {
				parallelLists.add(new ArrayList<String>());
			}
			/* when divideOnTestTime(), the end might be bigger than test.size() */
			if (end > tests.size()) {
				end = tests.size();
			}
			parallelLists.get(listIndex).addAll(tests.subList(start, end));
			listIndex++;
			start = end;
		}
	}
}