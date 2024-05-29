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
	private Arguments arg;
	private TestTarget tt;
	private List<String> testsToExecute;
	private List<String> testsToDisplay;
	private int numOfTests;
	private String parallelmk;
	private int defaultAvgTestTime;

	public TestDivider(Arguments arg, TestTarget tt) {
		this.arg = arg;
		this.tt = tt;
		testsToExecute = TestInfo.getTestsToExecute();
		testsToDisplay = TestInfo.getTestsToDisplay();
		numOfTests = TestInfo.numOfTests();
		parallelmk = arg.getProjectRootDir() + "/TKG/" + Constants.PARALLELMK;
		defaultAvgTestTime = 40000; // in milliseconds
	}

	private void divideOnTestTime(List<List<String>> parallelLists, List<Integer> testListTime, int testTime, Queue<Map.Entry<String, Integer>> durationQueue) {
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
	}

	private void divideOnMachineNum(List<List<String>> parallelLists, List<Integer> testListTime, int numOfMachines, Queue<Map.Entry<String, Integer>> durationQueue) {
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
	}

	private String constructURL(String impl, String plat, String group, String level) {
		int limit = 10; // limit the number of builds used to calculate the average duration
		String URL = (arg.getTRSSURL().isEmpty() ? Constants.TRSS_URL : arg.getTRSSURL()) + "/api/getTestAvgDuration?limit=" + limit + "&jdkVersion=" + arg.getJdkVersion() + "&impl=" + impl + "&platform=" + plat;

		if (tt.isSingleTest()) {
			URL += "&testName=" + tt.getTestTargetName();
		} else if (tt.isCategory()) {
			if (!group.equals("")) {
				URL += "&group=" + group;
			}
			if (!level.equals("")) {
				URL += "&level=" + level;
			}
		}
		return URL;
	}

	private String getGroup() {
		String group = "";
		if (tt.isCategory()) {
			for (String g : Constants.ALLGROUPS) {
				if (tt.getTestCategory().getCategorySet().contains(g)) {
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

	private String getLevel() {
		String level = "";
		if (tt.isCategory()) {
			for (String l : Constants.ALLLEVELS) {
				if (tt.getTestCategory().getCategorySet().contains(l)) {
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

	private void parseDuration(Reader reader, Map<String, Integer> map) throws IOException,ParseException {
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

	private Map<String, Integer> getDataFromFile() {
		String impl = arg.getBuildImpl();
		String plat = arg.getPlat();
		if (impl.equals("") || plat.equals("")) {
			return null;
		}
		String group = getGroup();
		Map<String, Integer> map = new HashMap<String, Integer>();
		System.out.println("Attempting to get test duration data from cached files.");
		String fileName = "";
		if (group.equals("")) {
			fileName = "Test_openjdk" + arg.getJdkVersion() + "_" + impl + "_(" + String.join("|", Constants.ALLGROUPS) + ")_" + plat + ".json";
		} else {
			fileName = "Test_openjdk" + arg.getJdkVersion() + "_" + impl + "_" + group + "_" + plat + ".json";
		}

		File directory = new File(arg.getProjectRootDir() + "/TKG/" + Constants.TRSSCACHE_DIR);
		Pattern p = Pattern.compile(fileName);
		File[] files = directory.listFiles(new FileFilter() {
			public boolean accept(File f) {
				return p.matcher(f.getName()).matches();
			}
		});
		if (files != null) {
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
		}
		return map;
	}

	private Map<String, Integer> getDataFromTRSS()  {
		String impl = arg.getBuildImpl();
		String plat = arg.getPlat();
		if (impl.equals("") || plat.equals("")) {
			return null;
		}
		String group = getGroup();
		String level = getLevel();
		Map<String, Integer> map = new HashMap<String, Integer>();
		String URL = constructURL(impl, plat, group, level);
		String command = "curl --silent --max-time 120 -L " + URL;
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
			e.printStackTrace();
		}
		return map;
	}

	private void printDefaultTime() {
		System.out.println("(Default duration assigned, executed tests: " + defaultAvgTestTime / 1000 + "s; not executed tests: 0s.)");
	}

	private Set<String> matchCollections(List<String> list, Map<String, Integer> map) {
		if (list == null || map == null) return new HashSet<String>();
		Set<String> set = new HashSet<>(map.keySet());
		set.retainAll(list);
		return set;
	}

	private Queue<Map.Entry<String, Integer>> createDurationQueue() {
		Queue<Map.Entry<String, Integer>> durationQueue = new PriorityQueue<>(
			(a, b) -> a.getValue() == b.getValue() ? b.getKey().compareTo(a.getKey()) : b.getValue().compareTo(a.getValue())
		);

		List<String> allTests = new ArrayList<String>();
		allTests.addAll(testsToExecute);
		allTests.addAll(testsToDisplay);
		Set<String> executeSet = new HashSet<String>(testsToExecute);
		Set<String> displaySet = new HashSet<String>(testsToDisplay);
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
				} else {
					durationQueue.offer(new AbstractMap.SimpleEntry<>(test, defaultAvgTestTime));
					testsInvalid.put(test, duration);
				}
			} else {
				int defaultTime = 0;
				if (executeSet.contains(test)) {
					defaultTime = defaultAvgTestTime;
				} else if (displaySet.contains(test)) {
					defaultTime = 0;
				}
				durationQueue.offer(new AbstractMap.SimpleEntry<>(test, defaultTime));
				testsNotFound.add(test);
			}
		}

		System.out.println("\nTEST DURATION");
		System.out.println("====================================================================================");
		System.out.println("Total number of tests searched: " + numOfTests);
		int foundNum = numOfTests - testsNotFound.size() - testsInvalid.size();
		System.out.println("Number of test durations found: " + foundNum);
		if (foundNum == 0) {
			System.out.println("No test duration data found.");
			printDefaultTime();
		} else {
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
		}
		System.out.println("====================================================================================");

		return durationQueue;
	}

	private String formatTime(int milliSec) {
		int sec = milliSec / 1000;
		String duration = String.format("%02dm%02ds", sec / 60, sec % 60);
		return duration;
	}

	private void writeParallelmk(List<List<String>> parallelLists) {
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

	private void printParallelStatus(List<List<String>> parallelLists, List<Integer> testListTime) {
		int maxListTime = 0;
		int totalTime = 0;
		for (int listTime : testListTime) {
			maxListTime = maxListTime > listTime ? maxListTime : listTime;
			totalTime += listTime;
		}
		System.out.println("Reducing estimated test running time from " + formatTime(totalTime) + " to " + formatTime(maxListTime) + ".\n");

		for (int i = 0; i < parallelLists.size(); i++) {
			System.out.println("-------------------------------------testList_" + i + "-------------------------------------");
			System.out.println("Number of tests: " + parallelLists.get(i).size());
			System.out.println("Estimated running time: " + formatTime(testListTime.get(i)));
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

	private void divideTests(List<List<String>> parallelLists, List<Integer> testListTime) {
		Queue<Map.Entry<String, Integer>> durationQueue = createDurationQueue();
		if (arg.getNumOfMachines() == null) {
			if (arg.getTestTime() == null) {
				divideOnMachineNum(parallelLists, testListTime, 1, durationQueue);
			} else {
				divideOnTestTime(parallelLists, testListTime, arg.getTestTime() * 60 * 1000, durationQueue);
			}
		} else {
			divideOnMachineNum(parallelLists, testListTime, Math.max(1, Math.min(arg.getNumOfMachines(), testsToExecute.size())), durationQueue);
		}
	}

	public void generateLists() {
		if (numOfTests == 0) {
			System.out.println("No tests found for target: " + tt.getTestTargetName());
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

	public void clean() {
		File f = new File(parallelmk);
		f.delete();
	}
}