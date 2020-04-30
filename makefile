##############################################################################
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https:$(D)$(D)www.apache.org$(D)licenses$(D)LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##############################################################################

#
# Makefile to run various JVM tests
#

ifndef TEST_JDK_HOME
$(error Please provide TEST_JDK_HOME value.)
else
export TEST_JDK_HOME:=$(subst \,/,$(TEST_JDK_HOME))
export OLD_JAVA_HOME := $(JAVA_HOME)
export JAVA_HOME := $(TEST_JDK_HOME)
$(info JAVA_HOME is set to $(JAVA_HOME))
endif

D = /

SUBDIRS = ..

ifndef TEST_ROOT
	TEST_ROOT := $(shell pwd)$(D)..
endif

#######################################
# run test
#######################################
_TESTTARGET = $(firstword $(MAKECMDGOALS))
TESTTARGET = $(patsubst _%,%,$(_TESTTARGET))
ifneq (compile, $(_TESTTARGET))
ifneq (clean, $(_TESTTARGET))
ifneq (test, $(_TESTTARGET))
ifneq (genParallelList, $(_TESTTARGET))
ifneq (_failed, $(_TESTTARGET))
$(_TESTTARGET):
	$(MAKE) -f makeGen.mk AUTO_DETECT=$(AUTO_DETECT) MODE=tests TESTTARGET=$(TESTTARGET) TESTLIST=$(TESTLIST)
	$(MAKE) -f runtest.mk $(_TESTTARGET)
endif
endif
endif
endif
endif

#######################################
# compile tools and test materials
#######################################
compile: compileTools
	$(MAKE) -f compile.mk compile

#######################################
# compile tools
# If AUTO_DETECT is turned on, compile and execute envDetector in build_envInfo.xml.
#######################################
compileTools:
	ant -f .$(D)scripts$(D)build_tools.xml -DTEST_JDK_HOME=$(TEST_JDK_HOME)
ifneq ($(AUTO_DETECT), false)
	@echo "AUTO_DETECT is set to true"
	${TEST_JDK_HOME}$(D)bin$(D)java -cp .$(D)bin$(D)TestKitGen.jar org.openj9.envInfo.EnvDetector JavaInfo
else
	@echo "AUTO_DETECT is set to false"
endif

#######################################
# compile and run all tests
#######################################
test: compile _all
	@$(ECHO) "All Tests Completed"

.PHONY: test

.NOTPARALLEL: test

#######################################
# run failed tests
#######################################
_failed:
	@$(MAKE) -f failedtargets.mk _failed

.PHONY: _failed

.NOTPARALLEL: _failed

#######################################
# generate parallel list
#######################################
genParallelList: compileTools
	$(MAKE) -f makeGen.mk AUTO_DETECT=$(AUTO_DETECT) MODE=parallelList NUM_MACHINES=$(NUM_MACHINES) TEST_TIME=$(TEST_TIME) TESTTARGET=$(TEST) TESTLIST=$(TESTLIST)

#######################################
# clean
#######################################
clean:
	$(MAKE) -f clean.mk clean

.PHONY: clean
