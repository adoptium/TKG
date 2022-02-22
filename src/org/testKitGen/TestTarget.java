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

public class TestTarget {
	enum Type { 
		SINGLE, CATEGORY, LIST; 
	}

	private String testTarget = null;
	private Type type = Type.SINGLE;
	private TestList tl = null;
	private TestCategory tc = null;
	private static TestTarget instance;

	private TestTarget() {
		tl = TestList.getInstance();
		tc = TestCategory.getInstance();
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

	public boolean isCategory() {
		return type == Type.CATEGORY;
	}

	public boolean isSingleTest() {
		return type == Type.SINGLE;
	}

	public boolean isList() {
		return type == Type.LIST;
	}

	public TestList getTestList() {
		return tl;
	}

	public TestCategory getTestCategory() {
		return tc;
	}

	public boolean filterTestInfo(TestInfo testInfo) {
		switch (type) {
			case SINGLE:
				tl.filter(testInfo); 
				break;
			case CATEGORY:
				tc.filter(testInfo);
				break;
			case LIST:
				tl.filter(testInfo);
				break;
			default:
				System.err.println("Invalid test type: " + type);
		}
		for (Variation var : testInfo.getVars()) {
			if (var.getStatus() != Variation.PrintStatus.DO_NOT_PRINT) {
				return true;
			}
		}
		return false;
	}

	public void parse(String target, String list) {
		testTarget = target;
		if (testTarget.equals("testList") && list != null) {
			tl.parseTarget(list);
			type = Type.LIST;
		} else if (tc.parseTarget(target)) {
			type = Type.CATEGORY;
		} else {
			//for single test, testList is the target
			tl.parseTarget(target);
			type = Type.SINGLE;
		}
	}
}