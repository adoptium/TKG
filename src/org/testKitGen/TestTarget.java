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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestTarget {
	enum Type { 
		SINGLE, CATEGORY, LIST; 
	}
	enum Header {
		REGULAR, DISABLED, ECHODISABLED;
	}
	private static String testTarget = null;
	private static Type type = Type.SINGLE;
	private static Set<String> categorySet = new HashSet<String>();
	private static Header header = Header.REGULAR;
	private static String testList = null;
	private static Set<String> testSet = new HashSet<String>();

	public static String getTestTarget() {
		return testTarget;
	}

	public static Set<String> getTestSet() {
		return testSet;
	}

	public static boolean isCategory() {
		return type == Type.CATEGORY;
	}

	public static boolean isSingleTest() {
		return type == Type.SINGLE;
	}

	public static boolean isList() {
		return type == Type.LIST;
	}

	public static Set<String> getCategorySet() {
		return categorySet;
	}

	public static boolean isRegular() {
		return header == Header.REGULAR;
	}

	public static boolean isDisabled() {
		return header == Header.DISABLED;
	}

	public static boolean isEchoDisabled() {
		return header == Header.ECHODISABLED;
	}

	public static void parse(String target, String list) {
		testTarget = target;
		testList = list;
		if (testTarget.equals("testList") && testList != null) {
			parseList();
		} else {
			parseTarget();
		}
	}

	private static void parseList() {
		header = Header.REGULAR;
		type = Type.LIST;
		String[] testListArr = testList.split(",");
		Arrays.sort(testListArr);
		for (String test : testListArr) {
			Pattern p = Pattern.compile("^(.*)_\\d+$");
			Matcher n = p.matcher(test);
			if (n.find()) {
				if (testSet.contains(n.group(1))) {
					continue;
				}
			}
			testSet.add(test);
		}
	}

	private static void parseTarget() {
		String strippedTarget = testTarget;
		if (testTarget.startsWith("echo.disabled.")) {
			header = Header.ECHODISABLED;
			strippedTarget = testTarget.substring(new String("echo.disabled.").length());
		} else if (testTarget.startsWith("disabled.")) {
			header = Header.DISABLED;
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
			type = Type.CATEGORY;
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
				testSet.add(strippedTarget);
				type = Type.SINGLE;
			} else {
				type = Type.CATEGORY;
			}
		}
	}
}