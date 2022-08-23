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

public class TestInfo {
	private String testCaseName;
	private String command;
	private String platform;
	private List<String> platformRequirementsList;
	private List<Variation> vars;
	private Map<String, String> capabilities;
	private String aotOptions;
	private int iterations;
	private String levelStr;
	private Map<String, String> features;
	private List<String> levels;
	private List<String> groups;
	private List<String> types;
	private static List<String> testsToExecute = new ArrayList<>();
	private static List<String> testsToDisplay = new ArrayList<>();

	public TestInfo(Arguments arg) {
		this.testCaseName = null;
		this.command = null;
		this.platform = null;
		this.platformRequirementsList = new ArrayList<String>();
		this.vars = new ArrayList<Variation>();
		this.aotOptions = "";
		this.iterations = Integer.parseInt(arg.getIterations());
		this.capabilities = new HashMap<String, String>();
		this.levelStr = "";
		this.features = new HashMap<String, String>();
		this.levels = new ArrayList<String>();
		this.groups = new ArrayList<String>();
		this.types = new ArrayList<String>();
	}

	public String getTestCaseName() {
		return this.testCaseName;
	}

	public void setTestCaseName(String testCaseName) {
		this.testCaseName = testCaseName;
	}

	public List<Variation> getVars() {
		return this.vars;
	}

	public void setVars(List<Variation> vars) {
		this.vars = vars;
	}

	public String getCommand() {
		return this.command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getPlatform() {
		return this.platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public List<String> getPlatformRequirementsList() {
		return this.platformRequirementsList;
	}

	public void addPlatformRequirements(String platformRequirements) {
		this.platformRequirementsList.add(platformRequirements);
	}

	public Map<String, String> getCapabilities() {
		return this.capabilities;
	}

	public void setCapabilities(Map<String, String>  capabilities) {
		this.capabilities = capabilities;
	}

	public String getAotOptions() {
		return this.aotOptions;
	}

	public void setAotOptions(String aotOptions) {
		this.aotOptions = aotOptions;
	}

	public int getIterations() {
		return this.iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public String getLevelStr() {
		return this.levelStr;
	}

	public void setLevelStr(String levelStr) {
		this.levelStr = levelStr;
	}

	public Map<String, String> getFeatures() {
		return this.features;
	}

	public void addFeature(String key, String value) {
		this.features.put(key, value);
	}

	public List<String> getLevels() {
		return this.levels;
	}

	public void setLevels(List<String> levels) {
		this.levels = levels;
	}

	public List<String> getGroups() {
		return this.groups;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	public List<String> getTypes() {
		return this.types;
	}

	public void setTypes(List<String> types) {
		this.types = types;
	}

	public static void countTests(TestInfo ti, TestTarget tt) {
		for (Variation var : ti.getVars()) {
			String testName = var.getPrefix() + var.getSubTestName();
			if (var.isValid() && (var.getStatus() == Variation.PrintStatus.PRINT_CMD)) {
				testsToExecute.add(testName);
			} else if (var.getStatus() != Variation.PrintStatus.DO_NOT_PRINT) {
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