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
include moveDmp.mk
-include buildInfo.mk

#######################################
# compile all tests under $(TEST_ROOT)
#######################################
ifneq ($(DYNAMIC_COMPILE), true)
COMPILE_BUILD_LIST := ${BUILD_LIST}
else
COMPILE_BUILD_LIST := ${REFINED_BUILD_LIST}
endif
# TEST_FLAG can be empty, Windows ant does not like empty -D<param> values
ifneq ($(TEST_FLAG),)
TEST_FLAG_PARAM := -DTEST_FLAG=$(TEST_FLAG)
else
TEST_FLAG_PARAM :=
endif
COMPILE_CMD=ant -f scripts$(D)build_test.xml $(Q)-DTEST_ROOT=$(TEST_ROOT)$(Q) $(Q)-DBUILD_ROOT=$(BUILD_ROOT)$(Q) $(Q)-DJDK_VERSION=$(JDK_VERSION)$(Q) $(Q)-DJDK_IMPL=$(JDK_IMPL)$(Q) $(Q)-DJDK_VENDOR=$(JDK_VENDOR)$(Q) $(Q)-DJCL_VERSION=$(JCL_VERSION)$(Q) $(Q)-DBUILD_LIST=${COMPILE_BUILD_LIST}$(Q) $(Q)-DRESOURCES_DIR=${RESOURCES_DIR}$(Q) $(Q)-DSPEC=${SPEC}$(Q) $(Q)-DTEST_JDK_HOME=${TEST_JDK_HOME}$(Q) $(Q)-DJVM_VERSION=$(JVM_VERSION)$(Q) $(Q)-DLIB_DIR=$(LIB_DIR)$(Q) ${TEST_FLAG_PARAM}


compile:
	$(RM) -r $(COMPILATION_OUTPUT); \
	$(MKTREE) $(COMPILATION_OUTPUT); \
	($(COMPILE_CMD) 2>&1; echo $$? ) | tee $(Q)$(COMPILATION_LOG)$(Q); \
	$(MOVE_TDUMP)

.PHONY: compile
