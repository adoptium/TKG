##############################################################################
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##############################################################################

.PHONY: autoconfig autogen clean help

.DEFAULT_GOAL := autogen

help:
	@echo "This makefile is used to run perl script which generates makefiles for JVM tests before being built and executed."
	@echo "OPTS=help     Display help information for more options."

CURRENT_DIR := $(shell pwd)
OPTS=

D=/
P=:
Q="

TKG_JAR = .$(D)bin$(D)TestKitGen.jar
JSON_SIMPLE = $(LIB_DIR)$(D)json-simple.jar

ifneq (,$(findstring Win,$(OS)))
CURRENT_DIR := $(subst \,/,$(CURRENT_DIR))
P=;
endif
include $(CURRENT_DIR)$(D)featureSettings.mk
-include $(CURRENT_DIR)$(D)autoGenEnv.mk
include $(CURRENT_DIR)$(D)envSettings.mk

autoconfig:
	perl scripts$(D)configure.pl

# MSYS2_ARG_CONV_EXCL=* disables MSYS2's automatic POSIX-to-Win path
# rewriting of arguments handed to native Win32 binaries. The classpath
# entries below are already in Windows form (LIB_DIR was cygpath -w'd in
# the top-level makefile), so the rewriter would only corrupt them. We
# scope this to the recipes that actually need it instead of exporting
# globally, because the Apache Ant Unix wrapper script (used by other
# recipes) relies on the rewriter to translate ANT_HOME's POSIX path.
# Harmless under Cygwin (the variable is MSYS2-only).
autogen: autoconfig
	MSYS2_ARG_CONV_EXCL='*' ${TEST_JDK_HOME}/bin/java -cp $(Q)$(TKG_JAR)$(P)$(JSON_SIMPLE)$(Q) org.testKitGen.MainRunner --mode=$(MODE) --spec=$(SPEC) --microArch=$(Q)$(MICROARCH)$(Q) --osLabel=$(Q)$(OS_LABEL)$(Q) --jdkVersion=$(JDK_VERSION) --impl=$(JDK_IMPL) --vendor=$(Q)$(JDK_VENDOR)$(Q) --buildList=${BUILD_LIST} --iterations=$(TKG_ITERATIONS) --aotIterations=$(AOT_ITERATIONS) --testFlag=$(TEST_FLAG) --testTarget=$(TESTTARGET) --testList=$(TESTLIST) --numOfMachines=$(NUM_MACHINES) --testTime=$(TEST_TIME) --TRSSURL=$(TRSS_URL) $(OPTS)

AUTOGEN_FILES = $(wildcard $(CURRENT_DIR)$(D)jvmTest.mk)
AUTOGEN_FILES += $(wildcard $(CURRENT_DIR)$(D)machineConfigure.mk)
AUTOGEN_FILES += $(wildcard $(CURRENT_DIR)$(D)..$(D)*$(D)autoGenTest.mk)

clean:
	MSYS2_ARG_CONV_EXCL='*' ${TEST_JDK_HOME}/bin/java -cp $(Q)$(TKG_JAR)$(P)$(JSON_SIMPLE)$(Q) org.testKitGen.MainRunner --mode=$(MODE)

.PHONY: autoconfig autogen clean
