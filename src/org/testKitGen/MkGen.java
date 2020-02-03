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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MkGen {
	private File playlistXML;
	private List<String> currentdirs;
	private List<String> subdirs;
	private String makeFile;
	private List<TestInfo> testInfoArr;
	private String include;
	private Map<String, ArrayList<String>> testMap;

	public MkGen(File playlistXML, String absolutedir, ArrayList<String> currentdirs, List<String> subdirs) {
		this.playlistXML = playlistXML;
		this.currentdirs = currentdirs;
		this.subdirs = subdirs;
		this.makeFile = absolutedir + "/" + Constants.TESTMK;
		this.testInfoArr = new ArrayList<TestInfo>();
		this.testMap = new HashMap<String, ArrayList<String>>();
	}

	public boolean start() {
		File file = new File(makeFile); 
		file.delete();

		try {
			if (!subdirs.isEmpty()) {
				writeVars();
				if (processPlaylist()) {
					writeTargets();
				}
			} else {
				if (processPlaylist()) {
					writeVars();
					writeTargets();
				} else {
					return false;
				}
			}
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return true;
	}

	private void writeVars() throws IOException {
		System.out.println();
		System.out.println("Generating make file " + makeFile);
		FileWriter f = new FileWriter(makeFile);
		String realtiveRoot = "";
		int subdirlevel = currentdirs.size();
		if (subdirlevel == 0) {
			realtiveRoot = ".";
		} else {
			for (int i = 1; i <= subdirlevel; i++) {
				realtiveRoot = realtiveRoot + ((i == subdirlevel) ? ".." : "..$(D)");
			}
		}

		f.write(Constants.HEADERCOMMENTS + "\n");
		f.write("D=/\n\n");
		f.write("ifndef TEST_ROOT\n");
		f.write("\tTEST_ROOT := " + Options.getProjectRootDir() + "\n");

		f.write("endif\n\n");
		f.write("SUBDIRS = " + String.join(" ", subdirs) + "\n\n");
		f.write("include $(TEST_ROOT)$(D)TKG$(D)" + Constants.SETTINGSMK + "\n\n");
		f.close();
	}

	private boolean processPlaylist() throws SAXException, IOException, ParserConfigurationException {
		if (playlistXML == null) return false;

		Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(playlistXML);
		NodeList childNodes = xml.getDocumentElement().getChildNodes();

		for (int i = 0; i < childNodes.getLength(); i++) {
			Node currentNode = childNodes.item(i);

			if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
				Element currentElement = ((Element) currentNode);
				if (currentElement.getNodeName().equals("include")) {
					include = currentElement.getTextContent();
				} else if (currentElement.getNodeName().equals("test")) {
					TestInfo testInfo = new TestInfo(currentElement);
					if (testInfo.parseInfo()) {
						testInfoArr.add(testInfo);
					}
				}
			}
		}
		
		return testInfoArr.size() != 0;
	}

	private void writeSingleTest(TestInfo testInfo, FileWriter f) throws IOException {
		for (Variation var : testInfo.getVars()) {
			// Generate make target
			String name = var.getSubTestName();

			if (!testInfo.getCapabilities().isEmpty()) {
				List<String> capabilityReqs_HashKeys = new ArrayList<>(testInfo.getCapabilities().keySet());
				Collections.sort(capabilityReqs_HashKeys);
				for (String capa_key : capabilityReqs_HashKeys) {
					String condition_capsReqs = name + "_" + capa_key + "_CHECK";
					f.write(condition_capsReqs + "=$(" + capa_key + ")\n");
				}
			}

			String jvmtestroot = "$(JVM_TEST_ROOT)$(D)" + String.join("$(D)", currentdirs);
			f.write(name + ": TEST_RESROOT=" + jvmtestroot + "\n");
			f.write(name + ": JVM_OPTIONS?=" + testInfo.getAotOptions() + "$(RESERVED_OPTIONS) "
					+ (var.getJvmOptions().isEmpty() ? "" : (var.getJvmOptions() + " ")) + "$(EXTRA_OPTIONS)\n");

			f.write(name + ": TEST_GROUP=" + testInfo.getLevelStr() + "\n");
			String indent = "\t";
			f.write(name + ":\n");
			f.write(indent + "@echo \"\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
			f.write(indent
					+ "@echo \"===============================================\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
			f.write(indent + "@echo \"Running test $@ ...\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
			f.write(indent
					+ "@echo \"===============================================\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
			f.write(indent + "@perl '-MTime::HiRes=gettimeofday' -e 'print \"" + name
					+ " Start Time: \" . localtime() . \" Epoch Time (ms): \" . int (gettimeofday * 1000) . \"\\n\"' | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");

			if (testInfo.getDisabledReasons() != null) {
				// This line is also the key words to match runningDisabled
				f.write(indent
						+ "@echo \"Test is disabled due to:\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				for (String dReason : testInfo.getDisabledReasons()) {
					f.write(indent + "@echo \"" + dReason + "\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				}
			}

			if (var.isValid()) {
				List<String> capKeys = new ArrayList<String>(testInfo.getCapabilities().keySet());
				if (!capKeys.isEmpty()) {
					Collections.sort(capKeys);
					for (String cKey : capKeys) {
						String condition_capsReqs = name + "_" + cKey + "_CHECK";
						f.write("ifeq ($(" + condition_capsReqs + "), " + testInfo.getCapabilities().get(cKey) + ")\n");
					}
				}
	
				f.write(indent + "$(TEST_SETUP);\n");
	
				f.write(indent + "@echo \"variation: " + var.getVariation()
						+ "\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent
						+ "@echo \"JVM_OPTIONS: $(JVM_OPTIONS)\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
	
				f.write(indent + "{ ");
				for (int k = 1; k <= testInfo.getIterations(); k++) {
					f.write("itercnt=" + k + "; \\\n" + indent + "$(MKTREE) $(REPORTDIR); \\\n" + indent
							+ "$(CD) $(REPORTDIR); \\\n");
					f.write(indent + testInfo.getCommand() + ";");
					if (k != testInfo.getIterations()) {
						f.write(" \\\n" + indent);
					}
				}
				f.write(" } 2>&1 | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
	
				f.write(indent + "$(TEST_TEARDOWN);\n");
	
				if (!capKeys.isEmpty()) {
					Collections.sort(capKeys, Collections.reverseOrder());
					for (String cKey : capKeys) {
						f.write("else\n");
						f.write(indent + "@echo \"Skipped due to capabilities (" + cKey + ":"
								+ testInfo.getCapabilities().get(cKey)
								+ ") => $(TEST_SKIP_STATUS)\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
						f.write("endif\n");
					}
				}
			} else {
				if (testInfo.getPlatformRequirements() != null) {
					f.write(indent
							+ "@echo \"Skipped due to jvm options ($(JVM_OPTIONS)) and/or platform requirements ("
							+ testInfo.getPlatformRequirements()
							+ ") => $(TEST_SKIP_STATUS)\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				} else {
					f.write(indent
							+ "@echo \"Skipped due to jvm options ($(JVM_OPTIONS)) => $(TEST_SKIP_STATUS)\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				}
			}

			f.write(indent + "@perl '-MTime::HiRes=gettimeofday' -e 'print \"" + name
					+ " Finish Time: \" . localtime() . \" Epoch Time (ms): \" . int (gettimeofday * 1000) . \"\\n\"' | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q)\n");

			f.write("\n.PHONY: " + name + "\n\n");

			if (testInfo.getDisabledReasons() != null) {
				String echoName = "echo.disabled." + name;
				f.write(echoName + ":\n");
				f.write(indent + "@echo \"\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent
						+ "@echo \"===============================================\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@echo \"Running test " + name
						+ " ...\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent
						+ "@echo \"===============================================\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@perl '-MTime::HiRes=gettimeofday' -e 'print \"" + name
						+ " Start Time: \" . localtime() . \" Epoch Time (ms): \" . int (gettimeofday * 1000) . \"\\n\"' | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@echo \"" + name
						+ "_DISABLED\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@echo \"Disabled Reason:\"\n");

				for (String dReason : testInfo.getDisabledReasons()) {
					f.write(indent + "@echo \"" + dReason + "\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				}

				f.write(indent + "@perl '-MTime::HiRes=gettimeofday' -e 'print \"" + name
						+ " Finish Time: \" . localtime() . \" Epoch Time (ms): \" . int (gettimeofday * 1000) . \"\\n\"' | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q)\n");
				f.write("\n.PHONY: " + echoName + "\n\n");
			}
			updateTestMap(testInfo, name);
			if (!Options.isDisabledTarget() || testInfo.getDisabledReasons() != null) {
				 Utils.count();
			}
		}

		String testCaseName = testInfo.getTestCaseName();
		f.write(testCaseName + ":");
		for (Variation var : testInfo.getVars()) {
			f.write(" \\\n" + var.getSubTestName());
		}
		f.write("\n\n.PHONY: " + testCaseName + "\n\n");
	}

	private void updateTestMap(TestInfo testInfo, String name) {
		String testTarget = Options.getTestTarget();
		if (testInfo.getDisabledReasons() == null) {
			testMap.putIfAbsent(testTarget, new ArrayList<String>());
			testMap.get(testTarget).add(name);
		} else {
			String dlgtKey = "disabled." + testTarget;
			String echodlgtKey = "echo." + dlgtKey;
			String echoName = "echo.disabled." + name;
			testMap.putIfAbsent(dlgtKey, new ArrayList<String>());
			testMap.get(dlgtKey).add(name);
			testMap.putIfAbsent(echodlgtKey, new ArrayList<String>());
			testMap.get(echodlgtKey).add(echoName);
		}
	}

	private void writeTargets() throws IOException {
		FileWriter f = new FileWriter(makeFile, true);
		if (include != null) {
			f.write("-include " + include + "\n\n");
		}

		for (TestInfo testInfo : testInfoArr) {
			writeSingleTest(testInfo, f);
		}

		// If a test group is specified in target, generate the group target
		if (Options.getTestName() == null) {
			String testTarget = Options.getTestTarget();
			List<String> testTargets = Arrays.asList(testTarget, "disabled." + testTarget, "echo.disabled." + testTarget);

			for (String target : testTargets) {
				f.write(target + ":");
				if (testMap.containsKey(target)) {
					for (String eachTest : testMap.get(target)) {
						f.write(" \\\n" + eachTest);
					}
				}
				if (target.equals(testTarget)) {
					f.write(" \\\n" + testTargets.get(2));
				}
				f.write("\n\n.PHONY: " + target + "\n\n");
			}
		}
		f.close();
	}
}