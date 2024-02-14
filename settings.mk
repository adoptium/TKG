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

.PHONY: help rmResultFile resultsSummary

help:
	@echo "This makefile is used to build and execute JVM tests. You should specify the following"
	@echo "variables before using this script:"
	@echo "required:"
	@echo "    TEST_JDK_HOME=<path to JDK home directory that you wish to test>"
	@echo "    BUILD_LIST=<comma separated projects to be compiled and executed> (default to all projects)"
	@echo "optional:"
	@echo "    SPEC=[linux_x86-64|linux_x86-64_cmprssptrs|...] (platform on which to test, could be auto detected)"
	@echo "    JDK_VERSION=[8|9|10|11|12|Panama|Valhalla] (default to 8, could be auto detected)"
	@echo "    JDK_IMPL=[openj9|ibm|hotspot|sap] (default to openj9, could be auto detected)"
	@echo "    NATIVE_TEST_LIBS=<path to native test libraries> (default to native-test-libs folder at the same level as TEST_JDK_HOME)"

CD        = cd
ECHO      = echo
ECHO_NEWLINE = $(ECHO) $(Q)$(Q)
MKDIR     = mkdir
MKTREE    = mkdir -p
PWD       = pwd
EXECUTABLE_SUFFIX =
RUN_SCRIPT = sh
RUN_SCRIPT_STRING = "sh -c"
SCRIPT_SUFFIX=.sh
Q="
SQ='
P=:
AND_IF_SUCCESS=&&
PROPS_DIR=props_unix
TOTALCOUNT := 0

-include $(TEST_ROOT)$(D)TKG$(D)autoGenEnv.mk
include $(TEST_ROOT)$(D)TKG$(D)envSettings.mk
-include $(TEST_ROOT)$(D)TKG$(D)utils.mk
include $(TEST_ROOT)$(D)TKG$(D)testEnv.mk
include $(TEST_ROOT)$(D)TKG$(D)featureSettings.mk

ifndef JDK_VERSION
	export JDK_VERSION:=8
else
	export JDK_VERSION:=$(JDK_VERSION)
endif

ifndef TEST_JDK_HOME
$(error Please provide TEST_JDK_HOME value.)
else
export TEST_JDK_HOME := $(subst \,/,$(TEST_JDK_HOME))
endif

ifndef JRE_IMAGE
	ifneq (,$(findstring j2sdk-image,$(TEST_JDK_HOME)))
	JRE_ROOT := $(TEST_JDK_HOME)
	export JRE_IMAGE := $(subst j2sdk-image,j2re-image,$(JRE_ROOT))
	endif
endif

ifndef SPEC
$(error Please provide SPEC that matches the current platform (e.g. SPEC=linux_x86-64))
else
export SPEC:=$(SPEC)
endif

# temporarily support both JAVA_IMPL and JDK_IMPL
ifndef JDK_IMPL
	ifndef JAVA_IMPL
		export JDK_IMPL:=openj9
	else
		export JDK_IMPL:=$(JAVA_IMPL)
	endif
else
	export JDK_IMPL:=$(JDK_IMPL)
endif

export JDK_VENDOR:=$(JDK_VENDOR)

ifndef JVM_VERSION
	OPENJDK_VERSION = openjdk$(JDK_VERSION)

ifeq (hotspot, $(JDK_IMPL))
	JVM_VERSION = $(OPENJDK_VERSION)
else 
	JVM_VERSION = $(OPENJDK_VERSION)-$(JDK_IMPL)
endif

export JVM_VERSION:=$(JVM_VERSION)
endif

ifneq (,$(findstring win,$(SPEC)))
P=;
D=\\
EXECUTABLE_SUFFIX=.exe
RUN_SCRIPT="cmd /c" 
RUN_SCRIPT_STRING=$(RUN_SCRIPT)
SCRIPT_SUFFIX=.bat
PROPS_DIR=props_win
endif

# Environment variable OSTYPE is set to cygwin if running under cygwin.
ifndef CYGWIN
	OSTYPE?=$(shell echo $$OSTYPE)
	ifeq ($(OSTYPE),cygwin)
			CYGWIN:=1
	else
		CYGWIN:=0
	endif
endif

ifeq ($(CYGWIN),1)
	TEST_ROOT := $(shell cygpath -w $(TEST_ROOT))
endif
TEST_ROOT := $(subst \,/,$(TEST_ROOT))

ifndef BUILD_ROOT
BUILD_ROOT := $(TEST_ROOT)$(D)..$(D)jvmtest
BUILD_ROOT := $(subst \,/,$(BUILD_ROOT))
endif

JVM_TEST_ROOT = $(BUILD_ROOT)
TEST_GROUP=level.*

#######################################
# Set OS, ARCH and BITS based on SPEC
#######################################
WORD_LIST:= $(subst _, ,$(SPEC))
OS:=os.$(word 1,$(WORD_LIST))
ARCH_INFO:=$(word 2,$(WORD_LIST))

BITS=bits.32
ARCH=arch.$(ARCH_INFO)
ifneq (,$(findstring 64,$(ARCH_INFO)))
	BITS=bits.64
	ifneq (,$(findstring -64,$(ARCH_INFO)))
		ARCH:=arch.$(subst -64,,$(ARCH_INFO))
	else
		ARCH:=arch.$(subst 64,,$(ARCH_INFO))
	endif
else
	ifneq (,$(findstring 390,$(ARCH_INFO)))
		BITS=bits.31
	endif
endif

ifneq ($(DEBUG),)
$(info OS is set to $(OS), ARCH is set to $(ARCH) and BITS is set to $(BITS))
endif

DEFAULT_EXCLUDE=d.*.$(SPEC),d.*.$(ARCH),d.*.$(OS),d.*.$(BITS),d.*.generic-all
ifneq ($(DEBUG),)
$(info DEFAULT_EXCLUDE is set to $(DEFAULT_EXCLUDE))
endif

JAVA_COMMAND:=$(Q)$(TEST_JDK_HOME)$(D)bin$(D)java$(Q)
ifdef JRE_IMAGE
   JRE_COMMAND:=$(Q)$(JRE_IMAGE)$(D)bin$(D)java$(Q)
endif

#######################################
# common dir and jars
#######################################

TESTNG=$(LIB_DIR)$(D)testng.jar$(P)$(LIB_DIR)$(D)jcommander.jar
RESOURCES_DIR=$(JVM_TEST_ROOT)$(D)TestConfig$(D)resources

#######################################
# cmdlinetester jars
#######################################
CMDLINETESTER_JAR   =$(Q)$(JVM_TEST_ROOT)$(D)functional$(D)cmdline_options_tester$(D)cmdlinetester.jar$(Q)
CMDLINETESTER_RESJAR=$(Q)$(JVM_TEST_ROOT)$(D)functional$(D)cmdline_options_testresources$(D)cmdlinetestresources.jar$(Q)

#######################################
# testng report dir
#######################################
ifndef UNIQUEID
	GETID := $(TEST_ROOT)$(D)TKG$(D)scripts$(D)getUniqueId.pl
	export UNIQUEID := $(shell perl $(GETID) -v)
endif
TESTOUTPUT := $(TEST_ROOT)$(D)TKG$(D)output_$(UNIQUEID)

REPORTDIR_PREFIX = $(TESTOUTPUT)$(D)$@
REPORTDIR_NQ = $(REPORTDIR_PREFIX)
ifneq ($(TKG_ITERATIONS), 1)
	REPORTDIR_NQ = $(REPORTDIR_PREFIX)_ITER_$$itercnt
endif

ifneq (,$(findstring AOT,$(TEST_FLAG)))
	REPORTDIR_NQ = $(REPORTDIR_PREFIX)_AOT_ITER_$$aotItercnt
endif

REPORTDIR = $(Q)$(REPORTDIR_NQ)$(Q)

#######################################
# TEST_STATUS
#######################################
RM_REPORTDIR =
KEEP_REPORTDIR ?= true
ifeq ($(KEEP_REPORTDIR), false)
	RM_REPORTDIR = $(RM) -r $(REPORTDIR);
endif
ECHO_DIVIDER = $(ECHO) $(Q)-----------------------------------$(Q)
ECHO_PASSED = $(ECHO_DIVIDER); $(ECHO) $(Q)$@$(Q)$(Q)_PASSED$(Q); $(ECHO_DIVIDER)
ECHO_FAILED = $(ECHO_DIVIDER); $(ECHO) $(Q)$@$(Q)$(Q)_FAILED$(Q); $(ECHO_DIVIDER)

ifneq (,$(findstring AOT,$(TEST_FLAG)))
	ECHO_PASSED_AOT_ITER = $(ECHO_NEWLINE); $(ECHO) $(Q)$@$(Q)$(Q)_PASSED(AOT_ITER_$$aotItercnt)$(Q); $(ECHO_NEWLINE)
	ECHO_FAILED_AOT_ITER = $(ECHO_NEWLINE); $(ECHO) $(Q)$@$(Q)$(Q)_FAILED(AOT_ITER_$$aotItercnt)$(Q); $(ECHO_NEWLINE)
	AOT_ITER_COMPLETE = [ $$aotItercnt -eq $(AOT_ITERATIONS) ]
endif

ifneq ($(TKG_ITERATIONS), 1)
	ECHO_PASSED_ITER = $(ECHO_NEWLINE); $(ECHO) $(Q)$@$(Q)$(Q)_PASSED(ITER_$$itercnt)$(Q); $(ECHO_NEWLINE)
	ECHO_FAILED_ITER = $(ECHO_NEWLINE); $(ECHO) $(Q)$@$(Q)$(Q)_FAILED(ITER_$$itercnt)$(Q); $(ECHO_NEWLINE)
	HAS_FAILURE = [ $$success -ne $$itercnt ]
	ITER_COMPLETE = [ $$itercnt -eq $(TEST_ITERATIONS) ]
	ifeq ($(EXIT_FAILURE), true)
		EXIT_FAILURE_SUCCESS = $(HAS_FAILURE)
	endif
	ifeq ($(EXIT_SUCCESS), true)
		EXIT_FAILURE_SUCCESS = [ $$success -ne 0 ]
	endif
	ifneq (,$(findstring AOT,$(TEST_FLAG)))
		EXIT_CONDITION = ( $(AOT_ITER_COMPLETE) && $(ITER_COMPLETE) )
		ifdef EXIT_FAILURE_SUCCESS
			EXIT_CONDITION += || ( $(AOT_ITER_COMPLETE) && $(EXIT_FAILURE_SUCCESS) )
		endif
		TEST_STATUS= \
			if [ $$? -eq 0 ]; \
			then \
				$(ECHO_PASSED_AOT_ITER); \
				$(CD) $(TEST_ROOT); $(RM_REPORTDIR) \
				if $(AOT_ITER_COMPLETE); \
					then $(ECHO_PASSED_ITER); success=$$(( success+1 )); \
				fi; \
			else \
				$(ECHO_FAILED_AOT_ITER); \
				aotItercnt=$(AOT_ITERATIONS); \
				$(ECHO_FAILED_ITER); \
			fi; \
			if $(EXIT_CONDITION); \
			then \
				$(ECHO_DIVIDER); \
				$(ECHO) $(Q)(success rate: $$success/$$itercnt)$(Q); \
				if $(HAS_FAILURE); \
				then $(ECHO_FAILED);\
				else $(ECHO_PASSED); \
				fi; \
				itercnt=$(TKG_ITERATIONS); \
			fi
	else
		EXIT_CONDITION = $(ITER_COMPLETE)
		ifdef EXIT_FAILURE_SUCCESS
			EXIT_CONDITION += || $(EXIT_FAILURE_SUCCESS)
		endif
		TEST_STATUS = \
			if [ $$? -eq 0 ] ; \
			then $(ECHO_PASSED_ITER); success=$$(( success+1 )); $(CD) $(TEST_ROOT); $(RM_REPORTDIR) \
			else $(ECHO_FAILED_ITER); \
			fi; \
			if $(EXIT_CONDITION); \
			then \
				$(ECHO_DIVIDER); \
				$(ECHO) $(Q)(success rate: $$success/$$itercnt)$(Q); \
				if $(HAS_FAILURE); \
				then $(ECHO_FAILED);\
				else $(ECHO_PASSED); \
				fi; \
				itercnt=$(TKG_ITERATIONS); \
			fi
	endif
else
	ifneq (,$(findstring AOT,$(TEST_FLAG)))
		TEST_STATUS = \
			if [ $$? -eq 0 ]; \
			then \
				$(ECHO_PASSED_AOT_ITER); \
				if $(AOT_ITER_COMPLETE); \
				then $(ECHO_PASSED); \
				fi; \
				$(CD) $(TEST_ROOT); $(RM_REPORTDIR) \
			else \
				$(ECHO_FAILED_AOT_ITER); \
				$(ECHO_FAILED); \
				aotItercnt=$(AOT_ITERATIONS); \
			fi
	else
		TEST_STATUS = \
			if [ $$? -eq 0 ]; \
			then \
				$(ECHO_PASSED); \
				$(CD) $(TEST_ROOT); $(RM_REPORTDIR) \
			else \
				$(ECHO_FAILED); \
			fi
	endif
endif

ifneq ($(DEBUG),)
$(info TEST_STATUS is $(TEST_STATUS))
endif
TEST_SKIP_STATUS=$@_SKIPPED

#######################################
# TEST_SETUP
#######################################
TEST_SETUP=$(ECHO) "Nothing to be done for setup."
ifeq ($(JDK_IMPL), $(filter $(JDK_IMPL),openj9 ibm))
	TEST_SETUP=$(JAVA_COMMAND) -Xshareclasses:destroyAll; $(JAVA_COMMAND) -Xshareclasses:groupAccess,destroyAll; echo "cache cleanup done"
endif

#######################################
# TEST_TEARDOWN
#######################################
TEST_TEARDOWN=$(ECHO) "Nothing to be done for teardown."
ifeq ($(JDK_IMPL), $(filter $(JDK_IMPL),openj9 ibm))
	TEST_TEARDOWN=$(JAVA_COMMAND) -Xshareclasses:destroyAll; $(JAVA_COMMAND) -Xshareclasses:groupAccess,destroyAll; echo "cache cleanup done"
endif

#######################################
# include configure makefile
#######################################
ifndef MACHINE_MK
-include $(JVM_TEST_ROOT)$(D)TKG$(D)machineConfigure.mk
else
-include $(MACHINE_MK)$(D)machineConfigure.mk
endif

#######################################
# include openj9 specific settings
#######################################
ifeq ($(JDK_IMPL), $(filter $(JDK_IMPL),openj9 ibm))
	include $(TEST_ROOT)$(D)TKG$(D)openj9Settings.mk
endif

#######################################
# generic target
#######################################
_TESTTARGET = $(firstword $(MAKECMDGOALS))
TESTTARGET = $(patsubst _%,%,$(_TESTTARGET))

SUBDIRS_TESTTARGET = $(SUBDIRS:%=$(TESTTARGET)-%)

$(SUBDIRS_TESTTARGET):
	@if [ -f $(Q)$(@:$(TESTTARGET)-%=%)/autoGen.mk$(Q) ]; then \
		$(MAKE) -C $(@:$(TESTTARGET)-%=%) -f autoGen.mk $(TESTTARGET); \
	fi

$(TESTTARGET): $(SUBDIRS_TESTTARGET)

_$(TESTTARGET): setup_$(TESTTARGET) rmResultFile $(TESTTARGET) resultsSummary teardown_$(TESTTARGET)
	@$(ECHO) $@ done

.PHONY: _$(TESTTARGET) $(TESTTARGET) $(SUBDIRS) $(SUBDIRS_TESTTARGET)

.NOTPARALLEL: _$(TESTTARGET) $(TESTTARGET) $(SUBDIRS) $(SUBDIRS_TESTTARGET)

setup_%: testEnvSetup
	@$(ECHO)
	@$(ECHO) Running make $(MAKE_VERSION)
	@$(ECHO) set TEST_ROOT to $(TEST_ROOT)
	@$(ECHO) set JDK_VERSION to $(JDK_VERSION)
	@$(ECHO) set JDK_IMPL to $(JDK_IMPL)
	@$(ECHO) set JVM_VERSION to $(JVM_VERSION)
	@$(ECHO) set JCL_VERSION to $(JCL_VERSION)
	@if [ $(OLD_JAVA_HOME) ]; then \
		$(ECHO) JAVA_HOME was originally set to $(OLD_JAVA_HOME); \
	fi
	@$(ECHO) set JAVA_HOME to $(JAVA_HOME)
	@$(ECHO) set SPEC to $(SPEC)
	@$(ECHO) set TEST_FLAG to $(TEST_FLAG)
	@if [ $(MICROARCH) ]; then \
		$(ECHO) set MICROARCH to $(MICROARCH); \
	fi
	@if [ $(OS_LABEL) ]; then \
		$(ECHO) set OS_LABEL to $(OS_LABEL); \
	fi

	@$(MKTREE) $(Q)$(TESTOUTPUT)$(Q)
	@$(ECHO) Running $(TESTTARGET) ...
	@if [ $(TOTALCOUNT) -ne 0 ]; then \
		$(ECHO) There are $(TOTALCOUNT) test targets in $(TESTTARGET); \
	else \
		$(ECHO) There is no test for target $(TESTTARGET); \
	fi
	$(JAVA_COMMAND) -version

teardown_%: testEnvTeardown
	@$(ECHO)

ifndef JCL_VERSION
export JCL_VERSION:=latest
else
export JCL_VERSION:=$(JCL_VERSION)
endif

# Define the EXCLUDE_FILE to be used for temporarily excluding failed tests.
# This macro is used in /test/Utils/src/org/openj9/test/util/IncludeExcludeTestAnnotationTransformer
ifndef EXCLUDE_FILE
export EXCLUDE_FILE:=$(TEST_ROOT)$(D)TestConfig$(D)resources$(D)excludes$(D)$(JCL_VERSION)_exclude_$(JDK_VERSION).txt
# TODO: https://github.com/eclipse-openj9/openj9/issues/12811
OPENDJK_METHODHANDLES_ENABLED?=$(shell $(JAVA_COMMAND) -XshowSettings:properties -version 2>&1 | grep 'openjdk.methodhandles = true')
ifneq (,$(findstring true,$(OPENDJK_METHODHANDLES_ENABLED)))
	export EXCLUDE_FILE:=$(EXCLUDE_FILE),$(TEST_ROOT)$(D)TestConfig$(D)resources$(D)excludes$(D)feature_ojdkmh_exclude.txt
endif
# Issue to track excluded tests in x86-64_linux_vt_standard build: https://github.com/eclipse-openj9/openj9/issues/12878
VALUE_TYPE_STANDARD_BUILD?=$(shell $(JAVA_COMMAND) -version 2>&1 | grep 'vtstandard')
ifneq (,$(findstring vtstandard,$(VALUE_TYPE_STANDARD_BUILD)))
	export EXCLUDE_FILE:=$(EXCLUDE_FILE),$(TEST_ROOT)$(D)TestConfig$(D)resources$(D)excludes$(D)feature_vtstandard_exclude.txt
endif
endif

#######################################
# failed target
#######################################
FAILEDTARGETS = $(TEST_ROOT)$(D)TKG$(D)failedtargets.mk

#######################################
# result Summary
#######################################
TEMPRESULTFILE=$(TESTOUTPUT)$(D)TestTargetResult
PLATFROMFILE=$(TEST_ROOT)$(D)TKG$(D)resources$(D)buildPlatformMap.properties

ifndef DIAGNOSTICLEVEL
export DIAGNOSTICLEVEL:=failure
endif

rmResultFile:
	@$(RM) $(Q)$(TEMPRESULTFILE)$(Q)

resultsSummary:
	@$(ECHO_NEWLINE)
	@$(ECHO_NEWLINE)
	@$(ECHO) $(Q)All tests finished, run result summary:$(Q)
	$(CD) $(Q)$(TEST_ROOT)$(D)TKG$(D)scripts$(Q); \
	perl $(Q)resultsSum.pl$(Q) --failuremk=$(Q)$(FAILEDTARGETS)$(Q) --resultFile=$(Q)$(TEMPRESULTFILE)$(Q) --platFile=$(Q)$(PLATFROMFILE)$(Q) --diagnostic=$(DIAGNOSTICLEVEL) --jdkVersion=$(JDK_VERSION) --jdkImpl=$(JDK_IMPL) --jdkVendor=$(Q)$(JDK_VENDOR)$(Q) --spec=$(SPEC) --buildList=$(BUILD_LIST) --customTarget=$(Q)$(CUSTOM_TARGET)$(Q) --testTarget=$(TESTTARGET) --tapPath=$(TESTOUTPUT)$(D) --tapName=$(TAP_NAME) --comment=$(Q)$(RESULT_COMMENT)$(Q)
