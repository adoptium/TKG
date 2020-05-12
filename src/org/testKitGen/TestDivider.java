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
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.FileReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TestDivider {
	private static List<String> regularTests = new ArrayList<>();
	private static List<String> effortlessTests = new ArrayList<>();	// tests almost take no time
	private static boolean dataFromTRSS = false;
	private static Queue<Map.Entry<String, Integer>> testTimeQueue = new PriorityQueue<>(
		(a, b) -> a.getValue() == b.getValue() ? b.getKey().compareTo(a.getKey()) : b.getValue().compareTo(a.getValue())
	);
	private static List<Integer> testListTime = new ArrayList<>();
	private static String parallelmk = Options.getProjectRootDir() + "/TKG/" + Constants.PARALLELMK;
	private static List<List<String>> parallelLists = new ArrayList<>();
	private static int defaultAvgTestTime = 40000; // in milliseconds
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
		if (dataFromTRSS) {
			Queue<Map.Entry<Integer, Integer>> machineQueue = new PriorityQueue<>(
				(a, b) -> a.getValue() == b.getValue() ? a.getKey().compareTo(b.getKey()) : a.getValue().compareTo(b.getValue())
			);
			int limitFactor = testTime;
			int index = 0;
			while (!testTimeQueue.isEmpty()) {
				Map.Entry<String, Integer> testEntry = testTimeQueue.poll();
				String testName = testEntry.getKey();
				int testDuration = testEntry.getValue();
				if (!machineQueue.isEmpty() && (machineQueue.peek().getValue() + testDuration < limitFactor)) {
					Map.Entry<Integer, Integer> machineEntry = machineQueue.poll();
					parallelLists.get(machineEntry.getKey()).add(testName);
					int newTime = machineEntry.getValue() + testDuration;
					testListTime.set(machineEntry.getKey(), newTime);
					machineEntry.setValue(newTime);
					machineQueue.offer(machineEntry);
				} else {
					parallelLists.add(new ArrayList<String>());
					parallelLists.get(index).add(testName);
					testListTime.add(testDuration);
					if (testDuration < limitFactor) {
						Map.Entry<Integer,Integer> entry = new AbstractMap.SimpleEntry<>(index, testDuration);
						machineQueue.offer(entry);
					} else { 
						/* If the test time is greater than the limiting factor, set it as the new limiting factor. */
						limitFactor = testDuration;
						System.out.println("Warning: Test " + testName + " has duration " + formatTime(testDuration) + ", which is greater than the specified test list execution time " + testTime + "m. So this value is used to limit the overall execution time.");
					}
					index++;
					
				}
			}
		} else {
			int testsInEachList = testTime / defaultAvgTestTime;
			/* If a single test time is greater than allowed time on each machine, run 1 test per machine. */
			if (testsInEachList == 0) {
				testsInEachList = 1;
			}
			/* Populate regular tests first and append the effortless along to the regular test lists. */
			populateParallelLists(regularTests, testsInEachList, 0);

			/* If no regular test, no need to divide the effortless tests. */
			int numOfMachines = parallelLists.size() != 0 ? parallelLists.size() : 1;
			testsInEachList = effortlessTests.size() / numOfMachines;
			int remainder = effortlessTests.size() % numOfMachines;
			populateParallelLists(effortlessTests, testsInEachList, remainder);
		}
	}

	private static void divideOnMachineNum(int numOfMachines) {
		if (dataFromTRSS) {
			Queue<Map.Entry<Integer, Integer>> machineQueue = new PriorityQueue<>(
				(a, b) -> a.getValue() == b.getValue() ? a.getKey().compareTo(b.getKey()) : a.getValue().compareTo(b.getValue())
			);
			for (int i = 0; i < numOfMachines; i++) {
				parallelLists.add(new ArrayList<String>());
				testListTime.add(0);
				Map.Entry<Integer,Integer> entry = new AbstractMap.SimpleEntry<>(i, 0);
				machineQueue.offer(entry);
			}
			while (!testTimeQueue.isEmpty()) {
				Map.Entry<String, Integer> testEntry = testTimeQueue.poll();
				Map.Entry<Integer, Integer> machineEntry = machineQueue.poll();

				parallelLists.get(machineEntry.getKey()).add(testEntry.getKey());
				int newTime = machineEntry.getValue() + testEntry.getValue();
				testListTime.set(machineEntry.getKey(), newTime);
				machineEntry.setValue(newTime);
				machineQueue.offer(machineEntry);
			}
		} else {
			int testsInEachList = regularTests.size() / numOfMachines;
			int remainder = regularTests.size() % numOfMachines;
			populateParallelLists(regularTests, testsInEachList, remainder);

			testsInEachList = effortlessTests.size() / numOfMachines;
			remainder = effortlessTests.size() % numOfMachines;
			populateParallelLists(effortlessTests, testsInEachList, remainder);
		}
	}

	private static String constructURL() {
		int limit = 10; // limit the number of builds used to calculate the average duration 
		String URL = (Options.getTRSSURL().isEmpty() ? Constants.TRSS_URL : Options.getTRSSURL()) + "/api/getTestAvgDuration?limit=" + limit + "&jdkVersion=" + Options.getJdkVersion();

		String impl = Options.getImpl();
		if (impl.equals("openj9")) {
			impl = "j9";
		} else if (impl.equals("hotspot")) {
			impl = "hs";
		}
		URL += "&impl=" + impl;

		String plat = Options.getSpec();
		try (FileReader platReader = new FileReader(Constants.BUILDPLAT_JSON)) {
			Properties platProp = new Properties();
			platProp.load(platReader);
			plat = platProp.getProperty(Options.getSpec());
		} catch (IOException e) {
			// nothing to do
		}
		URL += "&platform=" + plat;

		if (TestTarget.isSingleTest()) {
			URL += "&testName=" + TestTarget.getTestTarget();
		} else if (TestTarget.isCategory()) {
			String group = "";
			for (String g : Constants.ALLGROUPS) {
				if (TestTarget.getCategorySet().contains(g)) {
					if (group.equals("")) {
						group = g;
					} else {
						group = "";
						break;
					}
				}
			}
			if (!group.equals("")) {
				URL += "&group=" + group; 
			}
			String level = "";
			for (String l : Constants.ALLLEVELS) {
				if (TestTarget.getCategorySet().contains(l)) {
					if (level.equals("")) {
						level = l;
					} else {
						level = "";
						break;
					}
				}
			}
			if (!level.equals("")) {
				URL += "&level=" + level; 
			}
		} 
		return URL;
	}


	private static Map<String, Integer> getMapFromTRSS() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		String URL = constructURL();
		String command = "curl --silent --max-time 120 " + URL;
		System.out.println("Attempting to get test duration data from TRSS:");
		System.out.println(command);
		try {
			Process process = Runtime.getRuntime().exec(command);
			InputStream responseStream = process.getInputStream();
			BufferedReader responseReader = new BufferedReader(new InputStreamReader(responseStream));
			JSONParser parser = new JSONParser();
			JSONObject jsonObject = (JSONObject) parser.parse(responseReader);
			JSONArray buckets = (JSONArray) jsonObject.get("buckets");
			if (buckets != null && buckets.size() != 0) {
				JSONArray bucket = (JSONArray) buckets.get(0);
				if (bucket != null) {
					for (int i = 0; i < bucket.size(); i++) {
						JSONObject testData = (JSONObject) bucket.get(i);
						String testName = (String) testData.get("_id");
						Number testDurationNum = (Number) testData.get("avgDuration");
						if (testName != null && testDurationNum != null) {
							Double testDuration = ((Number) testData.get("avgDuration")).doubleValue();
							map.put(testName, testDuration.intValue());
						}
					}
				}
			}
		} catch (IOException | ParseException e) {
			System.out.println("Warning: cannot get data from TRSS.");
		}

		return map;
	}

	private static void buildTestTimeQueue() {
		Map<String, Integer> TRSSMap = getMapFromTRSS();
		Map<String, Integer> actualTestMap = new HashMap<>();
		List<String> testsNotFound = new ArrayList<>();
		Map<String, Integer> testsInvalid = new HashMap<>();
		if (TRSSMap.size() != 0) {
			for (String test : regularTests) {
				if (TRSSMap.containsKey(test)) {
					int duration = TRSSMap.get(test);
					if (duration > 0) {
						actualTestMap.put(test, duration);
						dataFromTRSS = true;
					} else {
						actualTestMap.put(test, defaultAvgTestTime);
						testsInvalid.put(test, duration);
					}
				} else {
					actualTestMap.put(test, defaultAvgTestTime);
					testsNotFound.add(test);
				}
			}
			for (String test : effortlessTests) {
				if (TRSSMap.containsKey(test)) {
					int duration = TRSSMap.get(test);
					if (duration > 0) {
						actualTestMap.put(test, duration);
						dataFromTRSS = true;
					} else {
						actualTestMap.put(test, defaultAvgTestTime);
						testsInvalid.put(test, duration);
					}
				} else {
					actualTestMap.put(test, 0);
					testsNotFound.add(test);
				}
			}
			for (Map.Entry<String, Integer> entry : actualTestMap.entrySet()) {
				testTimeQueue.offer(entry);
			}
		}
		System.out.println("\nTEST DURATION");
		System.out.println("====================================================================================");
		int totalNum = regularTests.size() + effortlessTests.size();
		System.out.println("Total number of tests searched: " + totalNum);
		if (dataFromTRSS) {
			int foundNum = testTimeQueue.size() - testsNotFound.size() - testsInvalid.size();
			System.out.println("Number of test durations found: " + foundNum);
			System.out.println("Top slowest tests: ");
			List<Map.Entry<String, Integer>> lastTests = new ArrayList<>();
			for (int i = 0; i < Math.min(foundNum, 3); i++) {
				lastTests.add(testTimeQueue.poll());
			}
			for (int i = 0; i < lastTests.size(); i++) {
				System.out.println("\t" + formatTime(lastTests.get(i).getValue()) + " " + lastTests.get(i).getKey());
				testTimeQueue.offer(lastTests.get(i));
			}
			if (!testsNotFound.isEmpty()) {
				System.out.println("Following test duration data cannot be found: ");
				System.out.println("(Default duration assigned, executed tests: " + defaultAvgTestTime / 1000 + "s; skipped/disabled tests: 0s.)");
				System.out.println("\t" + testsNotFound.toString());
			}
			if (!testsInvalid.isEmpty()) {
				System.out.println("Following test duration data is not valid: ");
				System.out.println("(Default duration assigned, executed tests: " + defaultAvgTestTime / 1000 + "s; skipped/disabled tests: 0s.)");
				for (Map.Entry<String, Integer> entry : testsInvalid.entrySet()) {
					System.out.println("\t" + entry.getKey() + " : " + entry.getValue() + "ms");
				}
			}
		} else {
			System.out.println("No test duration data found.");
			System.out.println("(Default duration assigned, executed tests: " + defaultAvgTestTime / 1000 + "s; skipped/disabled tests: 0s.)");

		}
		System.out.println("====================================================================================");
	}

	private static String formatTime(int milliSec) {
		int sec = milliSec / 1000;
		String duration = String.format("%02dm%02ds", sec / 60, sec % 60);
		return duration;
	}

	public static void generateLists() {
		if (regularTests.size() == 0 && effortlessTests.size() == 0) {
			System.out.println("No tests found for target: " + TestTarget.getTestTarget());
			System.out.println("No parallel test lists generated.");
			return;
		}

		if (TestTarget.isDisabled()) {
			// TRSS does not contain test duration for running disabled test at this moment
			System.out.println("Warning: Test duration data cannot be found for executing disabled target.");
			System.out.println("(Default duration assigned, running tests: " + defaultAvgTestTime / 1000 + "s; skipped tests: 0s.)");
		} else {
			buildTestTimeQueue();
		}
	
		if (Options.getNumOfMachines() == null) {
			if (Options.getTestTime() == null) {
				divideOnMachineNum(1);
			} else {
				divideOnTestTime(Options.getTestTime() * 60 * 1000);
			}
		} else {
			divideOnMachineNum(Math.max(1, Math.min(Options.getNumOfMachines(), regularTests.size())));
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
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		System.out.println("\nTest target is split into " + parallelLists.size() + " lists.");

		if (dataFromTRSS) {
			int maxListTime = 0;
			int totalTime = 0;
			for (int listTime : testListTime) {
				maxListTime = maxListTime > listTime ? maxListTime : listTime;
				totalTime += listTime;
			}
			System.out.println("Reducing estimated test running time from " + formatTime(totalTime) + " to " + formatTime(maxListTime) + ".\n");
		}

		for (int i = 0; i < parallelLists.size(); i++) {
			
			System.out.println("-------------------------------------testList_" + i + "-------------------------------------");
			System.out.println("Number of tests: " + parallelLists.get(i).size());
			if (dataFromTRSS) {
				System.out.println("Estimated running time: " + formatTime(testListTime.get(i)));
			}
			System.out.print("TESTLIST=");
			for (int j = 0; j < parallelLists.get(i).size(); j++) {
				System.out.print(parallelLists.get(i).get(j));
				if (j != parallelLists.get(i).size() - 1) {
					System.out.print(",");
				}
			}
			System.out.print("\n------------------------------------------------------------------------------------");
			for (int j = 1; j < String.valueOf(i).length(); j++) {
				System.out.print("-");
			}
			System.out.println("\n");
		}
		System.out.println("Parallel test lists file (" + parallelmk + ") is generated successfully.");
	}

	private static void populateParallelLists(List<String> tests, int testInList, int remainder) {
		/* it's better to have adjacent tests stay archived together */
		int listIndex = 0;
		int start = 0;
		while (start < tests.size()) {
			int end = start + testInList;
			if (remainder > 0) {
				end += 1;
				remainder--;
			}
			if (listIndex >= parallelLists.size()) {
				parallelLists.add(new ArrayList<String>());
			}
			/* when divideOnTestTime(), the end might be greater than test.size() */
			if (end > tests.size()) {
				end = tests.size();
			}
			parallelLists.get(listIndex).addAll(tests.subList(start, end));
			listIndex++;
			start = end;
		}
	}
}