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

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

public class TestList {

	private String testList = null;
	private Map<String, Boolean> testMap = new HashMap<String, Boolean>();
	private static TestList instance;

	private TestList() {
	}

	public static TestList getInstance() {
		if (instance == null) {
			instance = new TestList();
		}
		return instance;
	}

	public void filter(TestInfo testInfo) {
		String name = testInfo.getTestCaseName();
		String disabledName = "disabled." + name;
		String echoDisabledName = "echo.disabled." + name;

		for (Variation var : testInfo.getVars()){
			String subName = var.getSubTestName();
			String disabledSubName = "disabled." + subName;
			String echoDisabledSubName = "echo.disabled." + subName;
			String found = matchTarget(testMap, name, subName);
			if (found != null) {
				if (!var.isDisabled()) {
					var.setStatus(Variation.PrintStatus.PRINT_CMD);
				} else {
					var.setStatus(Variation.PrintStatus.PRINT_DISABLED);
				}
				testMap.replace(name, true);
				testMap.replace(subName, true);
			}
			String foundEchoDisabled = matchTarget(testMap, echoDisabledName, echoDisabledSubName);
			if (foundEchoDisabled != null) {
				if (var.isDisabled()) {
					var.setStatus(Variation.PrintStatus.PRINT_DISABLED);
					testMap.replace(echoDisabledName, true);
					testMap.replace(echoDisabledSubName, true);
				}
				var.setPrefix("echo.disabled.");
			}
			String foundDisabled = matchTarget(testMap, disabledName, disabledSubName);
			if (foundDisabled != null) {
				if (var.isDisabled()) {
					var.setStatus(Variation.PrintStatus.PRINT_CMD);
					testMap.replace(disabledName, true);
					testMap.replace(disabledSubName, true);
				}
				var.setPrefix("disabled.");
			}
			targetCheck(found, foundDisabled);
			targetCheck(foundEchoDisabled, foundDisabled);
		}
	}

	private void targetCheck(String target1, String target2) {
		if ((target1 != null) && (target2 != null)) {
			System.err.println("Error: testList contains contradictory target: " + target1 + " and " + target2 + ".");
			System.exit(1);
		}
	}

	private String matchTarget(Map<String, Boolean> testMap, String name, String subName) {
		if (testMap.containsKey(name)) {
			return name;
		} else if (testMap.containsKey(subName)) {
			return subName;
		}
		return null;
	}

	public Map<String, Boolean> getTestMap() {
		return testMap;
	}

	public void parseTarget(String list) {
		testList = list;
		String[] testListArr = testList.split(",");

		Arrays.sort(testListArr);
		for (String test : testListArr) {
			testMap.put(test, false);
		}
	}
}