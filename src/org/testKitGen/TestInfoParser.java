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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TestInfoParser {
	private Arguments arg;
	private ModesDictionary md;
	private Element testEle;

	public TestInfoParser(Arguments arg, ModesDictionary md, Element testEle) {
		this.arg = arg;
		this.md = md;
		this.testEle = testEle;
	}

	public TestInfo parse() {
		TestInfo ti = new TestInfo(arg);

		ti.setTestCaseName(testEle.getElementsByTagName("testCaseName").item(0).getTextContent().trim());

		// Do not generate make taget if impl doesn't match the exported impl
		NodeList implNodes = testEle.getElementsByTagName("impl");

		boolean isValidImpl = implNodes.getLength() == 0;
		if (!isValidImpl) {
			List<String> impls = new ArrayList<String>();
			getElements(impls, "impls", "impl", Constants.ALLIMPLS, ti.getTestCaseName());
			isValidImpl = impls.contains(arg.getImpl());
		}
		if (!isValidImpl) return null;

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
		if (!isValidSubset) return null;

		// Do not generate make target if the test is AOT not applicable when test flag
		// is set to AOT
		NodeList aot = testEle.getElementsByTagName("aot");

		boolean isValidAot = true;

		if (aot.getLength() > 0 && arg.getTestFlag().contains("aot")) {
			isValidAot = !aot.item(0).getTextContent().trim().equals("nonapplicable");
		}
		if (!isValidAot) return null;

		NodeList preqNodes = testEle.getElementsByTagName("platformRequirements");
		if (preqNodes.getLength() > 0) {
			ti.setPlatformRequirements(preqNodes.item(0).getTextContent().trim());
		}

		List<String> variations = new ArrayList<String>();
		getElements(variations, "variations", "variation", null, ti.getTestCaseName());
		List<Variation> listOfVars = new ArrayList<Variation>();
		for (int i = 0; i < variations.size(); i++) {
			String subTestName = ti.getTestCaseName() + "_" + i;
			Variation var = parseVariation(subTestName, variations.get(i), ti.getPlatformRequirements());
			listOfVars.add(var);
		}
		if (variations.size() == 0) {
			String subTestName = ti.getTestCaseName() + "_0";
			Variation var = parseVariation(subTestName, "NoOptions", ti.getPlatformRequirements());
			listOfVars.add(var);
		}
		ti.setVars(listOfVars);

		List<String> levels = new ArrayList<String>();
		getElements(levels, "levels", "level", Constants.ALLLEVELS, ti.getTestCaseName());
		// level defaults to "extended"
		if (levels.size() == 0) {
			levels.add("extended");
		}
		String levelStr = "";
		for (int i = 0; i < levels.size(); i++) {
			if (!levelStr.isEmpty()) {
				levelStr += ",";
			}
			levelStr = levelStr + "level." + levels.get(i);
			ti.setLevelStr(levelStr);
		}
		ti.setLevels(levels);

		List<String> groups = new ArrayList<String>();
		getElements(groups, "groups", "group", Constants.ALLGROUPS, ti.getTestCaseName());
		// group defaults to "extended"
		if (groups.size() == 0) {
			groups.add("functional");
		}
		ti.setGroups(groups);

		List<String> types = new ArrayList<String>();
		getElements(types, "types", "type", Constants.ALLTYPES, ti.getTestCaseName());
		// type defaults to "regular"
		if (types.size() == 0) {
			types.add("regular");
		}
		ti.setTypes(types);

		ti.setCommand(testEle.getElementsByTagName("command").item(0).getTextContent().trim());

		NodeList capabilitiesNodes = testEle.getElementsByTagName("capabilities");
		if (capabilitiesNodes.getLength() > 0) {
			String[] capabilityReqs_Arr = capabilitiesNodes.item(0).getTextContent().split(",");
			Map<String, String> capabilities = new HashMap<String, String>();
			for (String capabilityReq : capabilityReqs_Arr) {
				String[] colonSplit = capabilityReq.trim().split(":");
				capabilities.put(colonSplit[0], colonSplit[1]);
			}
			ti.setCapabilities(capabilities);
		}

		NodeList aotNodes = testEle.getElementsByTagName("aot");
		if (arg.getTestFlag().contains("aot")) {
			// aot defaults to "applicable" when testFlag contains AOT
			if (aotNodes.getLength() == 0 || aotNodes.item(0).getTextContent().trim().equals("applicable")) {
				ti.setAotOptions("$(AOT_OPTIONS) ");
			} else if (aotNodes.item(0).getTextContent().trim().equals("explicit")) {
				// When test tagged with AOT explicit, its test command has AOT options and runs
				// multiple times explicitly
				ti.setIterations(1);
			}
		}

		parseDisableInfo(ti);
		return ti;
	}

	private void parseDisableInfo(TestInfo ti) {
		NodeList disabledNodes = testEle.getElementsByTagName("disabled");
		if (disabledNodes.getLength() == 0) return;
		for (int i = 0; i < disabledNodes.getLength(); i++) {
			Element disabled = (Element) disabledNodes.item(i);
			Node commentNode = disabled.getElementsByTagName("comment").item(0);
			String comment = "";
			if (commentNode != null) {
				comment = commentNode.getTextContent().trim();
			}
			//TODO: remove temporarily support for old style
			if (comment == "") {
				comment = disabledNodes.item(i).getTextContent().trim();
			}
			Node variationNode = disabled.getElementsByTagName("variation").item(0);
			String variation = null;
			if (variationNode != null) {
				variation = variationNode.getTextContent().trim();
			}
			for (Variation var : ti.getVars()) {
				if ((variation == null) || var.getVariation().equals(variation)) {
					var.addDisabledReasons(comment);
				}
			} 
		}
	}

	private void getElements(List<String> list, String parentTag, String childTag, List<String> all, String testName) {
		NodeList parents = testEle.getElementsByTagName(parentTag);
		if (parents.getLength() > 0) {
			Element parentElement = (Element) parents.item(0);
			NodeList children = parentElement.getElementsByTagName(childTag);
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				String value = child.getTextContent().trim();
				if ((all != null) && (!all.contains(value))) {
					System.err.println("Error: The " + childTag + ": " + value + " for test " + testName
							+ " is not valid, the valid " + childTag + " strings are " + joinStrList(all) + ".");
					System.exit(1);
				}
				list.add(child.getTextContent().trim());
			}

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

	private Variation parseVariation(String subTestName, String variation, String platformRequirements) {
		Variation var = new Variation(subTestName, variation);

		String jvmOptions = " " + variation + " ";

		jvmOptions = jvmOptions.replaceAll(" NoOptions ", "");

		Pattern pattern = Pattern.compile(".* Mode([^ ]*?) .*");
		Matcher matcher = pattern.matcher(jvmOptions);

		String mode = "";
		boolean isValid = true;
		if (matcher.matches()) {
			mode = matcher.group(1).trim();
			String clArgs = md.getClArgs(mode);
			List<String> invalidSpecs = md.getInvalidSpecs(mode);
			if (invalidSpecs.contains(arg.getSpec())) {
				isValid = false;
			}
			jvmOptions = jvmOptions.replace("Mode" + mode, clArgs);
		}
		jvmOptions = jvmOptions.trim();
		isValid &= checkPlatformReq(platformRequirements);

		var.setJvmOptions(jvmOptions);
		var.setMode(mode);
		var.setValid(isValid);
		return var;
	}

	private boolean checkPlatformReq(String platformRequirements) {
		if ((platformRequirements != null) && (!platformRequirements.trim().isEmpty())) {
			for (String pr : platformRequirements.split("\\s*,\\s*")) {
				pr = pr.trim();
				String[] prSplitOnDot = pr.split("\\.");
				String spec = arg.getSpec();
				String fullSpec = spec;

				// Special case 32/31-bit specs which do not have 32 or 31 in the name (i.e.
				// aix_ppc)
				if (!spec.contains("-64")) {
					if (spec.contains("390")) {
						fullSpec = spec + "-31";
					} else {
						fullSpec = spec + "-32";
					}
				}

				if (prSplitOnDot[0].charAt(0) == '^') {
					if (fullSpec.contains(prSplitOnDot[1])) {
						return false;
					}
				} else {
					if (!fullSpec.contains(prSplitOnDot[1])) {
						return false;
					}
				}
			}
		}
		return true;
	}
}