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

		// Do not generate make target if impl doesn't match the exported jdk_impl
		List<String> impls = new ArrayList<String>();
		getElements(impls, "impls", "impl", Constants.ALLIMPLS, ti.getTestCaseName());
		boolean isValidImpl = (impls.size() == 0) || impls.contains(arg.getImpl());
		if (!isValidImpl) return null;

		// Do not generate make target if vendor doesn't match the exported jdk_vendor
		List<String> vendors = new ArrayList<String>();
		getElements(vendors, "vendors", "vendor", null, ti.getTestCaseName());
		boolean isValidVendor = (vendors.size() == 0) || vendors.contains(arg.getVendor());
		if (!isValidVendor) return null;

		// Do not generate make target if version doesn't match the exported jdk_version
		List<String> versions = new ArrayList<String>();
		getElements(versions, "versions", "version", null, ti.getTestCaseName());
		boolean isValidVersion = versions.size() == 0;
		for (String version : versions) {
			isValidVersion = checkJavaVersion(version);
			if (isValidVersion) {
				break;
			}
		}
		if (!isValidVersion) return null;

		// Do not generate make target if the test is AOT not applicable when test flag
		// is set to AOT
		NodeList aot = testEle.getElementsByTagName("aot");

		boolean isValidAot = true;

		if (aot.getLength() > 0 && arg.getTestFlag().contains("aot")) {
			isValidAot = !aot.item(0).getTextContent().trim().equals("nonapplicable");
		}
		if (!isValidAot) return null;

		NodeList platNodes = testEle.getElementsByTagName("platform");
		if (platNodes.getLength() > 0) {
			ti.setPlatform(platNodes.item(0).getTextContent().trim());
		}

		NodeList preqNodes = testEle.getElementsByTagName("platformRequirements");
		if (preqNodes.getLength() > 0) {
			ti.setPlatformRequirements(preqNodes.item(0).getTextContent().trim());
		}

		List<String> variations = new ArrayList<String>();
		getElements(variations, "variations", "variation", null, ti.getTestCaseName());
		List<Variation> listOfVars = new ArrayList<Variation>();
		for (int i = 0; i < variations.size(); i++) {
			String subTestName = ti.getTestCaseName() + "_" + i;
			Variation var = parseVariation(subTestName, variations.get(i), ti.getPlatform(), ti.getPlatformRequirements());
			listOfVars.add(var);
		}
		if (variations.size() == 0) {
			String subTestName = ti.getTestCaseName() + "_0";
			Variation var = parseVariation(subTestName, "NoOptions", ti.getPlatform(), ti.getPlatformRequirements());
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

	private boolean checkJavaVersion(String version) {
		boolean rt = false;
		if (version.equalsIgnoreCase(arg.getJdkVersion())) {
			rt = true;
		} else {
			try {
				Pattern pattern = Pattern.compile("^(.*)\\+$");
				Matcher matcher = pattern.matcher(version);
				if (matcher.matches()) {
					if (Integer.parseInt(matcher.group(1)) <= Integer.parseInt(arg.getJdkVersion())) {
						rt = true;
					}
				}
			} catch (NumberFormatException e) {
				// Nothing to do
			}
		}
		return rt;
	}

	private void parseDisableInfo(TestInfo ti) {
		NodeList disabledNodes = testEle.getElementsByTagName("disabled");
		if (disabledNodes.getLength() == 0) return;
		for (int i = 0; i < disabledNodes.getLength(); i++) {
			Element disabled = (Element) disabledNodes.item(i);
			String comment = getDisabledEle(disabled, "comment", ti.getTestCaseName());
			if (comment == null) {
				System.err.println("Error: Please provide a comment inside disable element in test " + ti.getTestCaseName() + ".");
				System.exit(1);
			}

			String impl = getDisabledEle(disabled, "impl", ti.getTestCaseName());
			String vendor = getDisabledEle(disabled, "vendor", ti.getTestCaseName());
			String version = getDisabledEle(disabled, "version", ti.getTestCaseName());
			String platform = getDisabledEle(disabled, "platform", ti.getTestCaseName());
			if (platform == null) {
				platform = getDisabledEle(disabled, "plat", ti.getTestCaseName());
			}
			String variation = getDisabledEle(disabled, "variation", ti.getTestCaseName());

			for (Variation var : ti.getVars()) {
				if (((impl == null) || arg.getImpl().equals(impl))
					&& ((vendor == null) || arg.getVendor().equals(vendor))
					&& ((version == null) || checkJavaVersion(version))
					&& ((platform == null) || checkPlat(platform))
					&& ((variation == null) || var.getVariation().equals(variation))) {
					var.addDisabledReasons(comment);
				}
			}
		}
	}

	private boolean checkPlat(String plat) {
		if (plat == null) return true;
		Pattern pattern = Pattern.compile(plat);
		Matcher matcher = pattern.matcher(arg.getPlat());
		return matcher.matches();
	}

	private String getDisabledEle(Element disabled, String ele, String test) {
		String rt = null;
		NodeList nodes = disabled.getElementsByTagName(ele);
		if (nodes != null) {
			if (nodes.getLength() == 1) {
				rt = nodes.item(0).getTextContent().trim();
			} else if (nodes.getLength() > 1) {
				System.err.println("Error: Multiple " + ele + " elements are not allowed in a single disable block (test " + test + "). The elements inside the disable element are in AND relationship. If you want to disable more than one " + ele + "s, please add more disable blocks. Or remove " + ele + " element from the block to disable all " + ele + "s.");
				System.exit(1);
			}
		}
		return rt;
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

	private Variation parseVariation(String subTestName, String variation, String platform, String platformRequirements) {
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
		isValid &= checkPlat(platform);
		//TODO: remove platformRequirements
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