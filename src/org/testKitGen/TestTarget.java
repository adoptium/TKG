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

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class TestTarget {
	private static String testTarget = "";
	private static boolean isCategory = false;
	private static boolean isSingleTest = false;
	private static String testName = null;
	private static Set<String> categorySet = new HashSet<String>();
	private static boolean isDisabled = false;
	private static boolean isEchoDisabled = false;

	public static String getTestTarget() {
		return testTarget;
	}

	public static boolean isCategory() {
		return isCategory;
	}

	public static boolean isSingleTest() {
		return isSingleTest;
	}

	public static String getTestName() {
		return testName;
	}

	public static Set<String> getCategorySet() {
		return categorySet;
	}

	public static boolean isRegular() {
		return !isDisabled && !isEchoDisabled;
	}

	public static boolean isDisabled() {
		return isDisabled;
	}

	public static boolean isEchoDisabled() {
		return isEchoDisabled;
	}

	public static void parse(String target) {
		testTarget = target;
		String strippedTarget = target;
		if (testTarget.startsWith("echo.disabled.")) {
			isEchoDisabled = true;
			strippedTarget = testTarget.substring(new String("echo.disabled.").length());
		} else if (testTarget.startsWith("disabled.")) {
			isDisabled = true;
			strippedTarget = testTarget.substring(new String("disabled.").length());
		}

		List<Set<String>> sets = new ArrayList<Set<String>>();
		sets.add(new HashSet<String>(Constants.ALLLEVELS));
		sets.add(new HashSet<String>(Constants.ALLGROUPS));
		sets.add(new HashSet<String>(Constants.ALLTYPES));

		if (strippedTarget.equals("all") || strippedTarget.equals("")) {
			for (Set<String> set : sets) {
				categorySet.addAll(set);
			}
			isSingleTest = false;
			isCategory = true;
		} else {
			String[] targets = strippedTarget.split("\\.");
			int j = 0;
			for (int i = 0; i < sets.size(); i++) {
				if (j < targets.length && sets.get(i).contains(targets[j])) {
					categorySet.add(targets[j]);
					j++;
				} else {
					categorySet.addAll(sets.get(i));
				}
			}
			if (j != targets.length) {
				testName = strippedTarget;
				isSingleTest = true;
				isCategory = false;
			} else {
				isSingleTest = false;
				isCategory = true;
			}
		}
	}
}