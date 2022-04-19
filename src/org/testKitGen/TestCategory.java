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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestCategory {
	enum Prefix {
		REGULAR, DISABLED, ECHODISABLED;
	}

	private Set<String> categorySet = new HashSet<String>();
	private Prefix prefix = Prefix.REGULAR;
	private static TestCategory instance;

	private TestCategory() {
	}

	public static TestCategory getInstance() {
		if (instance == null) {
			instance = new TestCategory();
		}
		return instance;
	}

	public void filter(TestInfo testInfo) {
		List<List<String>> allCategories = new ArrayList<>();
		allCategories.add(testInfo.getGroups());
		allCategories.add(testInfo.getTypes());
		allCategories.add(testInfo.getLevels());

		int found = 0;
		for (List<String> cat : allCategories) {
			for (String s : cat) {
				if (categorySet.contains(s)) {
					found++;
					break;
				}
			}
		}
		if (found == allCategories.size()) {
			for (Variation var : testInfo.getVars()) {
				if (prefix == Prefix.REGULAR) {
					if (var.isDisabled()) {
						var.setStatus(Variation.PrintStatus.PRINT_DISABLED);
					} else {
						var.setStatus(Variation.PrintStatus.PRINT_CMD);
					}
				} else if (prefix == Prefix.DISABLED) {
					if (var.isDisabled()) {
						var.setStatus(Variation.PrintStatus.PRINT_CMD);
					} else {
						var.setStatus(Variation.PrintStatus.DO_NOT_PRINT);
					}
					var.setPrefix("disabled.");
				} else if (prefix == Prefix.ECHODISABLED) {
					if (var.isDisabled()) {
						var.setStatus(Variation.PrintStatus.PRINT_DISABLED);
					} else {
						var.setStatus(Variation.PrintStatus.DO_NOT_PRINT);
					}
					var.setPrefix("echo.disabled.");
				}
			}
		}
	}

	public Set<String> getCategorySet() {
		return categorySet;
	}

	public boolean parseTarget(String target) {
		String strippedTarget = target;
		if (target.startsWith("echo.disabled.")) {
			prefix = Prefix.ECHODISABLED;
			strippedTarget = target.substring(new String("echo.disabled.").length());
		} else if (target.startsWith("disabled.")) {
			prefix = Prefix.DISABLED;
			strippedTarget = target.substring(new String("disabled.").length());
		}
		
		Set<String> potentialSet = new HashSet<String>();
		List<Set<String>> sets = new ArrayList<Set<String>>();
		sets.add(new HashSet<String>(Constants.ALLLEVELS));
		sets.add(new HashSet<String>(Constants.ALLGROUPS));
		sets.add(new HashSet<String>(Constants.ALLTYPES));

		if (strippedTarget.equals("all") || strippedTarget.equals("")) {
			for (Set<String> set : sets) {
				potentialSet.addAll(set);
			}
		} else {
			String[] targets = strippedTarget.split("\\.");
			int j = 0;
			for (int i = 0; i < sets.size(); i++) {
				if (j < targets.length && sets.get(i).contains(targets[j])) {
					potentialSet.add(targets[j]);
					j++;
				} else {
					potentialSet.addAll(sets.get(i));
				}
			}
			if (j != targets.length) {
				return false;
			}
		}

		categorySet = potentialSet;
		return true;
	}
}