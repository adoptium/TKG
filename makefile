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
$(_TESTTARGET):
	$(MAKE) -f makeGen.mk AUTO_DETECT=$(AUTO_DETECT) TESTTARGET=$(TESTTARGET)
	$(MAKE) -f runtest.mk $(_TESTTARGET)
endif
endif

#######################################
# compile
# If AUTO_DETECT is turned on, compile and execute envDetector in build_envInfo.xml.
#######################################
compile:
	ant -f .$(D)scripts$(D)build_tools.xml -DTEST_JDK_HOME=$(TEST_JDK_HOME)
ifneq ($(AUTO_DETECT), false)
	@echo "AUTO_DETECT is set to true"
	${TEST_JDK_HOME}$(D)bin$(D)java -cp .$(D)bin$(D)TestKitGen.jar org.openj9.envInfo.EnvDetector JavaInfo
else
	@echo "AUTO_DETECT is set to false"
endif
	$(MAKE) -f compile.mk compile

#######################################
# clean
#######################################
clean:
	$(MAKE) -f clean.mk clean

.PHONY: clean
