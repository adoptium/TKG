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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MkGen {
	private Arguments arg;
	private TestTarget tt;
	private List<String> dirList;
	private List<String> subdirs;
	private String makeFile;
	private PlaylistInfo pli;

	public MkGen(Arguments arg, TestTarget tt, PlaylistInfo pli, String makeFile, List<String> dirList, List<String> subdirs) {
		this.arg = arg;
		this.tt = tt;
		this.dirList = dirList;
		this.subdirs = subdirs;
		this.makeFile = makeFile;
		this.pli = pli;
	}

	public void start() {
		writeVars();
		if (pli.containsTest()) {
			writeTargets();
		}
		System.out.println("Generated " + makeFile + "\n");
	}

	private void writeVars() {
		try (FileWriter f = new FileWriter(makeFile)) {
			String realtiveRoot = "";
			int subdirlevel = dirList.size();
			if (subdirlevel == 0) {
				realtiveRoot = ".";
			} else {
				for (int i = 1; i <= subdirlevel; i++) {
					realtiveRoot = realtiveRoot + ((i == subdirlevel) ? ".." : "..$(D)");
				}
			}

			f.write(Constants.HEADERCOMMENTS + "\n");
			f.write("-include testVars.mk\n\n");
			f.write("D=/\n\n");
			f.write("ifndef TEST_ROOT\n");
			f.write("\tTEST_ROOT := " + arg.getProjectRootDir() + "\n");

			f.write("endif\n\n");
			f.write("SUBDIRS = " + String.join(" ", subdirs) + "\n\n");
			f.write("include $(TEST_ROOT)$(D)TKG$(D)" + Constants.SETTINGSMK + "\n\n");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void writeSingleTest(List<String> testsInPlaylist, TestInfo testInfo, FileWriter f) throws IOException {
		for (Variation var : testInfo.getVars()) {
			// Generate make target
			String testTargetName = var.getSubTestName();
			String indent = "\t";

			if (var.getStatus() == Variation.PrintStatus.PRINT_CMD) {
				if (!testInfo.getCapabilities().isEmpty()) {
					List<String> capabilityReqs_HashKeys = new ArrayList<>(testInfo.getCapabilities().keySet());
					Collections.sort(capabilityReqs_HashKeys);
					for (String capa_key : capabilityReqs_HashKeys) {
						String condition_capsReqs = testTargetName + "_" + capa_key + "_CHECK";
						f.write(condition_capsReqs + "=$(" + capa_key + ")\n");
					}
				}

				String jvmtestroot = "$(JVM_TEST_ROOT)$(D)" + String.join("$(D)", dirList);
				f.write(testTargetName + ": TEST_RESROOT=" + jvmtestroot + "\n");
				f.write(testTargetName + ": JVM_OPTIONS?=" + testInfo.getAotOptions() + "$(RESERVED_OPTIONS) "
						+ (var.getJvmOptions().isEmpty() ? "" : (var.getJvmOptions() + " ")) + "$(EXTRA_OPTIONS)\n");

				f.write(testTargetName + ": TEST_GROUP=" + testInfo.getLevelStr() + "\n");
				f.write(testTargetName + ": TEST_ITERATIONS=" + testInfo.getIterations() + "\n");
				f.write(testTargetName + ": AOT_ITERATIONS=" + testInfo.getAotIterations() + "\n");
				
				// Set special openjdk problem list for JVM options that contains FIPS profile
				// This feature is ignored if TEST_FLAG contains FIPS
				if (arg.getBuildList().contains("openjdk")) {
					String jvmOpts = var.getJvmOptions();
					String customprofileStr = "-Dsemeru.customprofile=";
					if (!arg.getTestFlag().contains("FIPS") && !jvmOpts.isEmpty() && jvmOpts.contains(customprofileStr)) {
						String[] splited = jvmOpts.split("\\s+");
						for (int i = 0; i < splited.length; i++) {
							if (splited[i].contains(customprofileStr)) {
								String fipsProfile = splited[i].replace(customprofileStr, "").trim();
								f.write(testTargetName + ": FIPS_VARIATION_PROBLEM_LIST_FILE=-exclude:$(Q)$(JTREG_JDK_TEST_DIR)/ProblemList-" + fipsProfile + ".txt$(Q)\n");
								break;
							}
						}
					}
				}
				
				f.write(testTargetName + ":\n");
				f.write(indent + "@echo \"\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent
						+ "@echo \"===============================================\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@echo \"Running test $@ ...\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent
						+ "@echo \"===============================================\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@perl '-MTime::HiRes=gettimeofday' -e 'print \"" + testTargetName
						+ " Start Time: \" . localtime() . \" Epoch Time (ms): \" . int (gettimeofday * 1000) . \"\\n\"' | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");

				if (var.isDisabled()) {
					// This line is also the key words to match runningDisabled
					f.write(indent
							+ "@echo \"Test is disabled due to:\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
					for (String dReason : var.getDisabledReasons()) {
						f.write(indent + "@echo \"- " + dReason + "\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
					}
				}

				if (var.isValid()) {
					List<String> capKeys = new ArrayList<String>(testInfo.getCapabilities().keySet());
					if (!capKeys.isEmpty()) {
						Collections.sort(capKeys);
						for (String cKey : capKeys) {
							String condition_capsReqs = testTargetName + "_" + cKey + "_CHECK";
							f.write("ifeq ($(" + condition_capsReqs + "), " + testInfo.getCapabilities().get(cKey) + ")\n");
						}
					}

					f.write(indent + "@echo \"variation: " + var.getVariation()
							+ "\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
					f.write(indent
							+ "@echo \"JVM_OPTIONS: $(JVM_OPTIONS)\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
		
					f.write(indent + "{ \\\n");
					if (testInfo.getIterations() != 1) {
						f.write(indent + "success=0; \\\n");
						f.write(indent + "itercnt=1; \\\n");
						f.write(indent + "while [ $$itercnt -le " + testInfo.getIterations() + " ]; \\\n");
						f.write(indent + "do \\\n");
						f.write(indent + "echo \"\";");
						f.write(indent + "echo \"== ITERATION $$itercnt ==\"; \\\n");
					}
					f.write(indent + "echo \"\";");
					f.write(indent + "echo \"TEST SETUP:\"; \\\n");
					f.write(indent + "$(TEST_SETUP); \\\n");
					if (arg.getTestFlag().contains("aot")) {
						f.write(indent + "aotItercnt=1; \\\n");
						f.write(indent + "while [ $$aotItercnt -le " + testInfo.getAotIterations() + " ]; \\\n");
						f.write(indent + "do \\\n");
						f.write(indent + "echo \"\";");
						f.write(indent + "echo \"=== AOT ITERATION $$aotItercnt ===\"; \\\n");
					}
					f.write(indent + "$(MKTREE) $(REPORTDIR); \\\n");
					f.write(indent + "$(CD) $(REPORTDIR); \\\n");
					f.write(indent + "echo \"\";");
					f.write(indent + "echo \"TESTING:\"; \\\n");
					f.write(indent + testInfo.getCommand() + "; \\\n");
					if (arg.getTestFlag().contains("aot")) {
						f.write(indent + "aotItercnt=$$((aotItercnt+1)); \\\n");
						f.write(indent + "done; \\\n");
					}
					f.write(indent + "echo \"\";");
					f.write(indent + "echo \"TEST TEARDOWN:\"; \\\n");
					f.write(indent + "$(TEST_TEARDOWN); \\\n");
					if (testInfo.getIterations() != 1) {
						f.write(indent + "itercnt=$$((itercnt+1)); \\\n");
						f.write(indent + "done;");
					}
					f.write(indent + " } 2>&1 | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");

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
					if (!testInfo.getPlatformRequirementsList().isEmpty()) {
						f.write(indent
								+ "@echo \"Skipped due to jvm options ($(JVM_OPTIONS)) and/or platform requirements ("
								+ testInfo.getPlatformRequirementsList()
								+ ") => $(TEST_SKIP_STATUS)\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
					} else {
						f.write(indent
								+ "@echo \"Skipped due to jvm options ($(JVM_OPTIONS)) => $(TEST_SKIP_STATUS)\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
					}
				}

				f.write(indent + "@perl '-MTime::HiRes=gettimeofday' -e 'print \"" + testTargetName
						+ " Finish Time: \" . localtime() . \" Epoch Time (ms): \" . int (gettimeofday * 1000) . \"\\n\"' | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q)\n");

				f.write("\n.PHONY: " + testTargetName + "\n\n");

			} else if (var.getStatus() == Variation.PrintStatus.PRINT_DISABLED) {
				String printName = testTargetName;
				testTargetName = "echo.disabled." + testTargetName;
				f.write(testTargetName + ":\n");
				f.write(indent + "@echo \"\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent
						+ "@echo \"===============================================\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@echo \"Running test " + printName
						+ " ...\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent
						+ "@echo \"===============================================\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@perl '-MTime::HiRes=gettimeofday' -e 'print \"" + printName
						+ " Start Time: \" . localtime() . \" Epoch Time (ms): \" . int (gettimeofday * 1000) . \"\\n\"' | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@echo \"" + printName
						+ "_DISABLED\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				f.write(indent + "@echo \"Disabled Reason:\"\n");

				for (String dReason : var.getDisabledReasons()) {
					f.write(indent + "@echo \"" + dReason + "\" | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q);\n");
				}

				f.write(indent + "@perl '-MTime::HiRes=gettimeofday' -e 'print \"" + printName
						+ " Finish Time: \" . localtime() . \" Epoch Time (ms): \" . int (gettimeofday * 1000) . \"\\n\"' | tee -a $(Q)$(TESTOUTPUT)$(D)TestTargetResult$(Q)\n");
				f.write("\n.PHONY: " + testTargetName + "\n\n");
			}
			if (var.getStatus() != Variation.PrintStatus.DO_NOT_PRINT) {
				testsInPlaylist.add(testTargetName);
			}
		}
	}

	private void writeTargets() {
		try (FileWriter f = new FileWriter(makeFile, true)) {
			if (!pli.getIncludeList().isEmpty()) {
				for (String include : pli.getIncludeList()) {
					f.write("-include " + include + "\n\n");
				}
			}

			List<String> testsInPlaylist = new ArrayList<String>();
			for (TestInfo testInfo : pli.getTestInfoList()) {
				writeSingleTest(testsInPlaylist, testInfo, f);
			}
	
			if (!((testsInPlaylist.size() == 1) && testsInPlaylist.get(0).equals(tt.getTestTargetName()))) {
				f.write(tt.getTestTargetName() + ":");
				for (String eachTest : testsInPlaylist) {
					f.write(" \\\n" + eachTest);
				}
				f.write("\n\n.PHONY: " + tt.getTestTargetName() + "\n\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}	
	}
}