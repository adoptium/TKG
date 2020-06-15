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

import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.regex.*;
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
	private static boolean durationFound = false;
	private static String parallelmk = Options.getProjectRootDir() + "/TKG/" + Constants.PARALLELMK;
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

	private static void divideOnTestTime(List<List<String>> parallelLists, List<Integer> testListTime, int testTime, Queue<Map.Entry<String, Integer>> durationQueue) {
		if (durationFound) {
			Queue<Map.Entry<Integer, Integer>> machineQueue = new PriorityQueue<>(
				(a, b) -> a.getValue() == b.getValue() ? a.getKey().compareTo(b.getKey()) : a.getValue().compareTo(b.getValue())
			);
			int limitFactor = testTime;
			int index = 0;
			while (!durationQueue.isEmpty()) {
				Map.Entry<String, Integer> testEntry = durationQueue.poll();
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
			populateParallelLists(parallelLists, regularTests, testsInEachList, 0);

			/* If no regular test, no need to divide the effortless tests. */
			int numOfMachines = parallelLists.size() != 0 ? parallelLists.size() : 1;
			testsInEachList = effortlessTests.size() / numOfMachines;
			int remainder = effortlessTests.size() % numOfMachines;
			populateParallelLists(parallelLists, effortlessTests, testsInEachList, remainder);
		}
	}

	private static void divideOnMachineNum(List<List<String>> parallelLists, List<Integer> testListTime, int numOfMachines, Queue<Map.Entry<String, Integer>> durationQueue) {
		if (durationFound) {
			Queue<Map.Entry<Integer, Integer>> machineQueue = new PriorityQueue<>(
				(a, b) -> a.getValue() == b.getValue() ? a.getKey().compareTo(b.getKey()) : a.getValue().compareTo(b.getValue())
			);
			for (int i = 0; i < numOfMachines; i++) {
				parallelLists.add(new ArrayList<String>());
				testListTime.add(0);
				Map.Entry<Integer,Integer> entry = new AbstractMap.SimpleEntry<>(i, 0);
				machineQueue.offer(entry);
			}
			while (!durationQueue.isEmpty()) {
				Map.Entry<String, Integer> testEntry = durationQueue.poll();
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
			populateParallelLists(parallelLists, regularTests, testsInEachList, remainder);

			testsInEachList = effortlessTests.size() / numOfMachines;
			remainder = effortlessTests.size() % numOfMachines;
			populateParallelLists(parallelLists, effortlessTests, testsInEachList, remainder);
		}
	}

	private static String constructURL(String impl, String plat, String group, String level) {
		int limit = 10; // limit the number of builds used to calculate the average duration 
		String URL = (Options.getTRSSURL().isEmpty() ? Constants.TRSS_URL : Options.getTRSSURL()) + "/api/getTestAvgDuration?limit=" + limit + "&jdkVersion=" + Options.getJdkVersion() + "&impl=" + impl + "&platform=" + plat;

		if (TestTarget.isSingleTest()) {
			URL += "&testName=" + TestTarget.getTestTarget();
		} else if (TestTarget.isCategory()) {
			if (!group.equals("")) {
				URL += "&group=" + group; 
			}
			if (!level.equals("")) {
				URL += "&level=" + level; 
			}
		} 
		return URL;
	}

	private static String getImpl() {
		String impl = "";
		if (Options.getImpl().equals("openj9")) {
			impl = "j9";
		} else if (Options.getImpl().equals("hotspot")) {
			impl = "hs";
		}
		return impl;
	}

	private static String getPlat() {
		String plat = "";
		try (FileReader platReader = new FileReader(Constants.BUILDPLAT_JSON)) {
			Properties platProp = new Properties();
			platProp.load(platReader);
			plat = platProp.getProperty(Options.getSpec());
		} catch (IOException e) {
		}
		return plat;
	}

	private static String getGroup() {
		String group = "";
		if (TestTarget.isCategory()) {
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
		}
		return group;
	}

	private static String getLevel() {
		String level = "";
		if (TestTarget.isCategory()) {
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
		}
		return level;
	}

	private static void parseDuration(Reader reader, Map map) throws IOException,ParseException {
		JSONParser parser = new JSONParser();
		JSONObject jsonObject = (JSONObject) parser.parse(reader);
		JSONArray testLists = (JSONArray) jsonObject.get("testLists");
		if (testLists != null && testLists.size() != 0) {
			JSONArray testList = (JSONArray) testLists.get(0);
			if (testList != null) {
				for (int i = 0; i < testList.size(); i++) {
					JSONObject testData = (JSONObject) testList.get(i);
					String testName = (String) testData.get("_id");
					Number testDurationNum = (Number) testData.get("avgDuration");
					if (testName != null && testDurationNum != null) {
						Double testDuration = ((Number) testData.get("avgDuration")).doubleValue();
						map.put(testName, testDuration.intValue());
					}
				}
			}
		}
	}

	private static Map<String, Integer> getDataFromFile() {
		String impl = getImpl();
		String plat = getPlat();
		if (impl.equals("") || plat.equals("")) {
			return null;
		}
		String group = getGroup();
		String level = getLevel();
		Map<String, Integer> map = new HashMap<String, Integer>();
		System.out.println("Attempting to get test duration data from cached files.");
		String fileName = "";
		if (group.equals("")) {
			fileName = "Test_openjdk" + Options.getJdkVersion() + "_" + impl + "_(" + String.join("|", Constants.ALLGROUPS) + ")_" + plat + ".json";
		} else {
			fileName = "Test_openjdk" + Options.getJdkVersion() + "_" + impl + "_" + group + "_" + plat + ".json";
		}

		File directory = new File(Options.getProjectRootDir() + "/TKG/" + Constants.RESOURCE);
		Pattern p = Pattern.compile(fileName);
		File[] files = directory.listFiles(new FileFilter() {
			public boolean accept(File f) {
				return p.matcher(f.getName()).matches();
			}
		});

		for (File f : files) {
			System.out.println("Reading file: " + f.getName().toString());
			try (Reader reader = new FileReader(f)) {
				parseDuration(reader, map);
			} catch (IOException|ParseException e) {
				System.out.println("Warning: cannot get data from cached files.");
				// when there's exception, clear the map object and try to get data from TRSS
				map.clear();
				break;
			}
		}
		return map;
	}

	private static Map<String, Integer> getDataFromTRSS()  {
		String impl = getImpl();
		String plat = getPlat();
		if (impl.equals("") || plat.equals("")) {
			return null;
		}
		String group = getGroup();
		String level = getLevel();
		Map<String, Integer> map = new HashMap<String, Integer>();
		String URL = constructURL(impl, plat, group, level);
		String command = "curl --silent --max-time 120 " + URL;
		System.out.println("Attempting to get test duration data from TRSS.");
		System.out.println(command);
		Process process;
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			System.out.println("Warning: cannot get data from TRSS.");
			return map;
		}
		try	(InputStream responseStream = process.getInputStream();
			Reader responseReader = new BufferedReader(new InputStreamReader(responseStream))) {
			parseDuration(responseReader, map);
		} catch (IOException | ParseException e) {
			System.out.println("Warning: cannot parse data from TRSS.");
		}
		return map;
	}

	private static void printDefaultTime() {
		System.out.println("(Default duration assigned, executed tests: " + defaultAvgTestTime / 1000 + "s; not executed tests: 0s.)");
	}

	private static Set<String> matchCollections(List<String> list, Map<String, Integer> map) {
		if (list == null || map == null) return new HashSet<String>();
		Set<String> set = new HashSet(map.keySet());
		set.retainAll(list);
		return set;
	}

	private static Queue<Map.Entry<String, Integer>> createDurationQueue() {
		Queue<Map.Entry<String, Integer>> durationQueue = new PriorityQueue<>(
			(a, b) -> a.getValue() == b.getValue() ? b.getKey().compareTo(a.getKey()) : b.getValue().compareTo(a.getValue())
		);

		if (TestTarget.isDisabled()) {
			// TRSS does not contain test duration for running disabled test at this moment
			System.out.println("Warning: Test duration data cannot be found for executing disabled target.");
			printDefaultTime();
			return durationQueue;
		}

		List<String> allTests = new ArrayList<String>();
		allTests.addAll(regularTests);
		allTests.addAll(effortlessTests);
		Map<String, Integer> TRSSMap = getDataFromTRSS();
		Set<String> matchTRSS = matchCollections(allTests, TRSSMap);
		Map<String, Integer> cacheMap = getDataFromFile();
		Set<String> matchCache = matchCollections(allTests, cacheMap);

		List<String> testsNotFound = new ArrayList<>();
		Map<String, Integer> testsInvalid = new HashMap<>();
		for (String test : allTests) {
			if (matchTRSS.contains(test) || matchCache.contains(test)) {
				int duration = TRSSMap.containsKey(test) ? TRSSMap.get(test) : cacheMap.get(test);
				if (duration > 0) {
					durationQueue.offer(new AbstractMap.SimpleEntry<>(test, duration));
					durationFound = true;
				} else {
					durationQueue.offer(new AbstractMap.SimpleEntry<>(test, defaultAvgTestTime));
					testsInvalid.put(test, duration);
				}
			} else {
				durationQueue.offer(new AbstractMap.SimpleEntry<>(test, defaultAvgTestTime));
				testsNotFound.add(test);
			}
		}

		System.out.println("\nTEST DURATION");
		System.out.println("====================================================================================");
		int totalNum = regularTests.size() + effortlessTests.size();
		System.out.println("Total number of tests searched: " + totalNum);
		if (durationFound) {
			int foundNum = totalNum - testsNotFound.size() - testsInvalid.size();
			System.out.println("Number of test durations found: " + foundNum);
			System.out.println("Top slowest tests: ");
			List<Map.Entry<String, Integer>> lastTests = new ArrayList<>();
			for (int i = 0; i < Math.min(foundNum, 3); i++) {
				lastTests.add(durationQueue.poll());
			}
			for (int i = 0; i < lastTests.size(); i++) {
				System.out.println("\t" + formatTime(lastTests.get(i).getValue()) + " " + lastTests.get(i).getKey());
				durationQueue.offer(lastTests.get(i));
			}
			if (!testsNotFound.isEmpty()) {
				System.out.println("Following test duration data cannot be found: ");
				printDefaultTime();
				System.out.println("\t" + testsNotFound.toString());
			}
			if (!testsInvalid.isEmpty()) {
				System.out.println("Following test duration data is not valid: ");
				printDefaultTime();
				for (Map.Entry<String, Integer> entry : testsInvalid.entrySet()) {
					System.out.println("\t" + entry.getKey() + " : " + entry.getValue() + "ms");
				}
			}
		} else {
			System.out.println("No test duration data found.");
			printDefaultTime();
		}
		System.out.println("====================================================================================");
		return durationQueue;
	}

	private static String formatTime(int milliSec) {
		int sec = milliSec / 1000;
		String duration = String.format("%02dm%02ds", sec / 60, sec % 60);
		return duration;
	}

	private static void writeParallelmk(List<List<String>> parallelLists) {
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
	}

	private static void printParallelStatus(List<List<String>> parallelLists, List<Integer> testListTime) {
		if (durationFound) {
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
			if (durationFound) {
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
	}

	private static void divideTests(List<List<String>> parallelLists, List<Integer> testListTime) {
		Queue<Map.Entry<String, Integer>> durationQueue = createDurationQueue();
		if (Options.getNumOfMachines() == null) {
			if (Options.getTestTime() == null) {
				divideOnMachineNum(parallelLists, testListTime, 1, durationQueue);
			} else {
				divideOnTestTime(parallelLists, testListTime, Options.getTestTime() * 60 * 1000, durationQueue);
			}
		} else {
			divideOnMachineNum(parallelLists, testListTime, Math.max(1, Math.min(Options.getNumOfMachines(), regularTests.size())), durationQueue);
		}
	}
	public static void generateLists() {
		if (regularTests.size() == 0 && effortlessTests.size() == 0) {
			System.out.println("No tests found for target: " + TestTarget.getTestTarget());
			System.out.println("No parallel test lists generated.");
			return;
		}

		List<List<String>> parallelLists = new ArrayList<>();
		List<Integer> testListTime = new ArrayList<>();
		divideTests(parallelLists, testListTime);
		writeParallelmk(parallelLists);
		printParallelStatus(parallelLists, testListTime);
		System.out.println("Parallel test lists file (" + parallelmk + ") is generated successfully.");
	}

	private static void populateParallelLists(List<List<String>> parallelLists, List<String> tests, int testInList, int remainder) {
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