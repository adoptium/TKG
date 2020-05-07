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

.DEFAULT_GOAL := compile

D = /

ifndef TEST_ROOT
	TEST_ROOT := $(shell pwd)$(D)..
endif

include settings.mk

#######################################
# compile all tests under $(TEST_ROOT)
#######################################
COMPILATION_LOG=$(TESTOUTPUT)$(D)compile$(D)compilation.log
MOVE_TDUMP_PERL=perl scripts$(D)moveDmp.pl --compileLogPath=$(Q)$(COMPILATION_LOG)$(Q) --testRoot=$(Q)$(TEST_ROOT)$(Q) --spec=$(Q)$(SPEC)$(Q)
MOVE_TDUMP=if [ -z $$(tail -n 1 $(COMPILATION_LOG) | grep 0) ]; then $(MOVE_TDUMP_PERL); $(RM) $(Q)$(COMPILATION_LOG)$(Q); false; else $(RM) $(Q)$(COMPILATION_LOG)$(Q); fi
COMPILE_CMD=ant -f scripts$(D)build_test.xml -DTEST_ROOT=$(TEST_ROOT) -DBUILD_ROOT=$(BUILD_ROOT) -DJDK_VERSION=$(JDK_VERSION) -DJDK_IMPL=$(JDK_IMPL) -DJCL_VERSION=$(JCL_VERSION) -DBUILD_LIST=${BUILD_LIST} -DRESOURCES_DIR=${RESOURCES_DIR} -DSPEC=${SPEC} -DTEST_JDK_HOME=${TEST_JDK_HOME} -DJVM_VERSION=$(JVM_VERSION) -DLIB_DIR=$(LIB_DIR)

compile:
	$(MKTREE) $(TESTOUTPUT)$(D)compile; \
	($(COMPILE_CMD) 2>&1; echo $$? ) | tee $(Q)$(COMPILATION_LOG)$(Q); \
	$(MOVE_TDUMP)

.PHONY: compile