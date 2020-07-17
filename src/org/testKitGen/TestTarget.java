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
import java.util.ListIterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestTarget {
	enum Type { 
		SINGLE, CATEGORY, LIST; 
	}
	enum Prefix {
		REGULAR, DISABLED, ECHODISABLED;
	}
	private String testTarget = null;
	private Type type = Type.SINGLE;
	private Set<String> categorySet = new HashSet<String>();
	private Prefix prefix = Prefix.REGULAR;
	private String testList = null;
	private Set<String> testSet = new HashSet<String>();
	private static TestTarget instance;

	private TestTarget() {
	}

	public static TestTarget getInstance() {
		if (instance == null) {
			instance = new TestTarget();
		}
		return instance;
	}

	public String getTestTargetName() {
		return testTarget;
	}

	private boolean filterOnDisabled(TestInfo testInfo) {
		if (isEchoDisabled() && !testInfo.isDisabled()) return false;
		//todo: remove isCategory when single target disabled behavior matches category target
		if (isCategory() && isDisabled() && !testInfo.isDisabled()) return false;
		return true;
	}

	private boolean filterOnTestName(TestInfo testInfo) {
		if (!testSet.contains(testInfo.getTestCaseName())) {
			ListIterator<Variation> iter = testInfo.getVars().listIterator();
			while(iter.hasNext()){
				String subTestName = iter.next().getSubTestName();
				if (testSet.contains(subTestName)) {
					testSet.remove(subTestName);
				} else {
					iter.remove();
				}
			}
			if (testInfo.getVars().isEmpty()) {
				return false;
			}
		} else {
			testSet.remove(testInfo.getTestCaseName());
		}
		return true;
	}

	private boolean filterOnTestCategory(TestInfo testInfo) {
		List<List<String>> allCategories = new ArrayList<>();
		allCategories.add(testInfo.getGroups());
		allCategories.add(testInfo.getTypes());
		allCategories.add(testInfo.getLevels());
		for (List<String> cat : allCategories) {
			boolean found = false;
			for (String s : cat) {
				if (getCategorySet().contains(s)) {
					found = true;
					break;
				}
			}
			if (!found) return false;
		}
		return true;
	}

	public boolean filterTestInfo(TestInfo testInfo) {
		boolean rt = true;
		switch (type) {
			case SINGLE:
				rt &= filterOnDisabled(testInfo);
				rt &= filterOnTestName(testInfo);
				break;
			case CATEGORY:
				rt &= filterOnDisabled(testInfo);
				rt &= filterOnTestCategory(testInfo);
				break;
			case LIST:
				rt &= filterOnTestName(testInfo);
				break;
			default:
				System.err.println("Invalid test type: " + type);
		}
		return rt;
	}

	public boolean isExecutedTarget(TestInfo testInfo) {
		boolean rt = false;
		if (isEchoDisabled()) {
			rt = false;
		} else if (isSingleTest()) {
			rt = true;
		} else if (isList()) {
			rt = !testInfo.isDisabled();
		} else if (isCategory()) {
			rt = (isRegular() && !testInfo.isDisabled()) || (isDisabled() && testInfo.isDisabled());
		}
		return rt;
	}

	public Set<String> getTestSet() {
		return testSet;
	}

	public boolean isCategory() {
		return type == Type.CATEGORY;
	}

	public boolean isSingleTest() {
		return type == Type.SINGLE;
	}

	public boolean isList() {
		return type == Type.LIST;
	}

	public Set<String> getCategorySet() {
		return categorySet;
	}

	public boolean isRegular() {
		return prefix == Prefix.REGULAR;
	}

	public boolean isDisabled() {
		return prefix == Prefix.DISABLED;
	}

	public boolean isEchoDisabled() {
		return prefix == Prefix.ECHODISABLED;
	}

	public void parse(String target, String list) {
		testTarget = target;
		testList = list;
		if (testTarget.equals("testList") && testList != null) {
			parseList();
		} else {
			parseTarget();
		}
	}

	private void parseList() {
		prefix = Prefix.REGULAR;
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

	private void parseTarget() {
		String strippedTarget = testTarget;
		if (testTarget.startsWith("echo.disabled.")) {
			prefix = prefix.ECHODISABLED;
			strippedTarget = testTarget.substring(new String("echo.disabled.").length());
		} else if (testTarget.startsWith("disabled.")) {
			prefix = Prefix.DISABLED;
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