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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class TestInfo {
	private Arguments arg;
	private ModesDictionary md;
	private Element testEle;
	private String testCaseName;
	private String command;
	private String platformRequirements;
	private List<Variation> vars;
	private Map<String, String> capabilities;
	private String aotOptions;
	private int iterations;
	private String levelStr;
	private List<String> levels;
	private List<String> groups;
	private List<String> types;
	private String[] disabledReasons;
	private static List<String> testsToExecute = new ArrayList<>();
	private static List<String> testsToDisplay = new ArrayList<>();

	public TestInfo(Arguments arg, ModesDictionary md, Element testEle) {
		this.arg = arg;
		this.md = md;
		this.testEle = testEle;
		this.testCaseName = null;
		this.command = null;
		this.platformRequirements = null;
		this.vars = new ArrayList<Variation>();
		this.aotOptions = "";
		this.iterations = Integer.parseInt(arg.getIterations());
		this.capabilities = new HashMap<String, String>();
		this.levelStr = "";
		this.levels = new ArrayList<String>();
		this.groups = new ArrayList<String>();
		this.types = new ArrayList<String>();
		this.disabledReasons = null;
	}

	public boolean parseInfo() {
		if (!validate()) return false;

		command = testEle.getElementsByTagName("command").item(0).getTextContent().trim();

		NodeList capabilitiesNodes = testEle.getElementsByTagName("capabilities");
		if (capabilitiesNodes.getLength() > 0) {
			String[] capabilityReqs_Arr = capabilitiesNodes.item(0).getTextContent().split(",");
			for (String capabilityReq : capabilityReqs_Arr) {
				String[] colonSplit = capabilityReq.trim().split(":");
				capabilities.put(colonSplit[0], colonSplit[1]);
			}
		}

		NodeList aotNodes = testEle.getElementsByTagName("aot");
		if (arg.getTestFlag().contains("aot")) {
			// aot defaults to "applicable" when testFlag contains AOT
			if (aotNodes.getLength() == 0 || aotNodes.item(0).getTextContent().trim().equals("applicable")) {
				aotOptions = "$(AOT_OPTIONS) ";
			} else if (aotNodes.item(0).getTextContent().trim().equals("explicit")) {
				// When test tagged with AOT explicit, its test command has AOT options and runs
				// multiple times explicitly
				iterations = 1;
			}
		}

		countTests();
		return true;
	}

	private void getElements(List<String> list, String tag, List<String> all) {
		NodeList nodes = testEle.getElementsByTagName(tag);
		for (int i = 0; i < nodes.getLength(); i++) {
			String value = nodes.item(i).getTextContent().trim();
			if (!all.contains(value)) {
				System.err.println("The " + tag + ": " + value + " for test " + testCaseName
						+ " is not valid, the valid " + tag + " strings are " + joinStrList(all) + ".");
				System.exit(1);
			}
			list.add(nodes.item(i).getTextContent().trim());
		}
	}

	private String joinStrList(List<String> list) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			sb.append(list.get(i));
			if (i != list.size() - 1) {
				sb.append("\t");
			}
		}
		return sb.toString();
	}

	private boolean validate() {
		this.testCaseName = testEle.getElementsByTagName("testCaseName").item(0).getTextContent().trim();

		// Do not generate make taget if impl doesn't match the exported impl
		NodeList implNodes = testEle.getElementsByTagName("impl");

		boolean isValidImpl = implNodes.getLength() == 0;
		if (!isValidImpl) {
			List<String> impls = new ArrayList<String>();
			getElements(impls, "impl", Constants.ALLIMPLS);
			isValidImpl = impls.contains(arg.getImpl());
		}
		if (!isValidImpl) return false;

		// Do not generate make target if subset doesn't match the exported jdk_version
		NodeList subsets = testEle.getElementsByTagName("subset");

		boolean isValidSubset = subsets.getLength() == 0;

		for (int j = 0; j < subsets.getLength(); j++) {
			String subset = subsets.item(j).getTextContent().trim();

			if (subset.equalsIgnoreCase(arg.getJdkVersion())) {
				isValidSubset = true;
				break;
			} else {
				try {
					Pattern pattern = Pattern.compile("^(.*)\\+$");
					Matcher matcher = pattern.matcher(subset);
					if (matcher.matches()) {
						if (Integer.parseInt(matcher.group(1)) <= Integer.parseInt(arg.getJdkVersion())) {
							isValidSubset = true;
							break;
						}
					}
				} catch (NumberFormatException e) {
					// Nothing to do
				}
			}
		}
		if (!isValidSubset) return false;

		// Do not generate make target if the test is AOT not applicable when test flag
		// is set to AOT
		NodeList aot = testEle.getElementsByTagName("aot");

		boolean isValidAot = true;

		if (aot.getLength() > 0 && arg.getTestFlag().contains("aot")) {
			isValidAot = !aot.item(0).getTextContent().trim().equals("nonapplicable");
		}
		if (!isValidAot) return false;

		NodeList preqNodes = testEle.getElementsByTagName("platformRequirements");
		if (preqNodes.getLength() > 0) {
			platformRequirements = preqNodes.item(0).getTextContent().trim();
		}

		NodeList variationsNodes = testEle.getElementsByTagName("variation");
		for (int i = 0; i < variationsNodes.getLength(); i++) {
			String subTestName = testCaseName + "_" + i;
			vars.add(new Variation(arg, md, subTestName, variationsNodes.item(i).getTextContent(), platformRequirements));
		}
		if (vars.size() == 0) {
			String subTestName = testCaseName + "_0";
			vars.add(new Variation(arg, md, subTestName, "NoOptions", platformRequirements));
		}

		if (TestTarget.isSingleTest() || TestTarget.isList()) {
			// If a test name is specified in target, only generate the matching make target
			if (!TestTarget.getTestSet().contains(testCaseName)) {
				List<Variation> temp = new ArrayList<Variation>();
				for (Variation v : vars) {
					if (TestTarget.getTestSet().contains(v.getSubTestName())) {
						temp.add(v);
						TestTarget.getTestSet().remove(v.getSubTestName());
					}
				}
				if (!temp.isEmpty()) {
					vars = temp;
				} else {
					return false;
				}
			} else {
				TestTarget.getTestSet().remove(testCaseName);
			}
		}

		getElements(levels, "level", Constants.ALLLEVELS);
		// level defaults to "extended"
		if (levels.size() == 0) {
			levels.add("extended");
		}
		for (int i = 0; i < levels.size(); i++) {
			if (!levelStr.isEmpty()) {
				levelStr += ",";
			}
			levelStr = levelStr + "level." + levels.get(i);
		}
		if (!checkTestCategory(levels)) return false;

		getElements(groups, "group", Constants.ALLGROUPS);
		// group defaults to "extended"
		if (groups.size() == 0) {
			groups.add("functional");
		}
		if (!checkTestCategory(groups)) return false;

		getElements(types, "type", Constants.ALLTYPES);
		// type defaults to "regular"
		if (types.size() == 0) {
			types.add("regular");
		}
		if (!checkTestCategory(types)) return false;

		NodeList disabledNodes = testEle.getElementsByTagName("disabled");
		if (TestTarget.isEchoDisabled() && (disabledNodes.getLength() == 0)) return false;
		if (TestTarget.isCategory() && TestTarget.isDisabled() && disabledNodes.getLength() == 0) return false;
		if (disabledNodes.getLength() > 0) {
			disabledReasons = disabledNodes.item(0).getTextContent().split("[\t\n]");
		}

		return true;
	}

	private boolean checkTestCategory(List<String> category) {
		if (!TestTarget.isCategory()) return true;
		// If category(level/group/type) is specificed in target, only generate matching make taget
		for (String s : category) {
			if (TestTarget.getCategorySet().contains(s)) {
				return true;
			}
		}
		return false;
	}

	public String getTestCaseName() {
		return this.testCaseName;
	}

	public List<Variation> getVars() {
		return this.vars;
	}

	public String getCommand() {
		return this.command;
	}

	public String getPlatformRequirements() {
		return this.platformRequirements;
	}

	public Map<String, String> getCapabilities() {
		return this.capabilities;
	}

	public String getAotOptions() {
		return this.aotOptions;
	}

	public int getIterations() {
		return this.iterations;
	}

	public String getLevelStr() {
		return this.levelStr;
	}

	public List<String> getLevels() {
		return this.levels;
	}

	public List<String> getGroups() {
		return this.groups;
	}

	public List<String> getTypes() {
		return this.types;
	}

	public String[] getDisabledReasons() {
		return this.disabledReasons;
	}

	public boolean isDisabled() {
		return getDisabledReasons() != null;
	}

	public boolean genCmd() {
		boolean rt = false;
		if (TestTarget.isEchoDisabled()) {
			rt = false;
		} else if (TestTarget.isSingleTest()) {
			rt = true;
		} else if (TestTarget.isList()) {
			rt = !isDisabled();
		} else if (TestTarget.isCategory()) {
			rt = (TestTarget.isRegular() && !isDisabled()) || (TestTarget.isDisabled() && isDisabled());
		}
		return rt;
	}

	public void countTests() {
		for (Variation var : vars) {
			String testName = var.getSubTestName();
			if (var.isValid() && genCmd()) {
				testsToExecute.add(testName);
			} else {
				testsToDisplay.add(testName);
			}
		}
	}

	public static List<String> getTestsToExecute() {
		return testsToExecute;
	}

	public static List<String> getTestsToDisplay() {
		return testsToDisplay;
	}

	public static int numOfTests() {
		return testsToExecute.size() + testsToDisplay.size();
	}
}