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

#
# This makefile contains openJ9 specific settings
#

#######################################
# Set JAVA_SHARED_LIBRARIES_DIR  and VM_SUBDIR for tests which need native library.
# Set ADD_JVM_LIB_DIR_TO_LIBPATH as tests on some platforms need LIBPATH containing VM directory
#######################################
TEST_JRE_BIN:=$(TEST_JDK_HOME)$(D)jre$(D)bin
TEST_JRE_LIB_DIR:=$(TEST_JRE_BIN)$(D)..$(D)lib
TEST_JDK_BIN:=$(TEST_JDK_HOME)$(D)bin
TEST_JDK_LIB_DIR:=$(TEST_JDK_BIN)$(D)..$(D)lib

VM_SUBDIR=default
ifneq (,$(findstring _cmprssptrs,$(SPEC)))
VM_SUBDIR=compressedrefs
endif

ifneq (,$(findstring le,$(SPEC)))
	ARCH_DIR=ppc64le
else ifneq (,$(findstring ppc-64,$(SPEC)))
	ARCH_DIR=ppc64
else ifneq (,$(findstring ppc,$(SPEC)))
	ARCH_DIR=ppc
else ifneq (,$(findstring arm,$(SPEC)))
	ARCH_DIR=arm
else ifneq (,$(findstring 390-64,$(SPEC)))
	ARCH_DIR=s390x
else ifneq (,$(findstring 390,$(SPEC)))
	ARCH_DIR=s390
else ifneq (,$(findstring linux_x86-64,$(SPEC)))
	ARCH_DIR=amd64
else ifneq (,$(findstring linux_x86,$(SPEC)))
	ARCH_DIR=i386
else ifneq (,$(findstring aarch64,$(SPEC)))
	ARCH_DIR=aarch64
endif

ifndef NATIVE_TEST_LIBS
	NATIVE_TEST_LIBS=$(TEST_JDK_HOME)$(D)..$(D)native-test-libs$(D)
endif

# if JCL_VERSION is current check for default locations for native test libs
# otherwise, native test libs are under NATIVE_TEST_LIBS
ifneq (, $(findstring current, $(JCL_VERSION)))
	ifeq (8, $(JDK_VERSION))
		ifneq (,$(findstring win,$(SPEC)))
			JAVA_SHARED_LIBRARIES_DIR:=$(TEST_JRE_BIN)$(D)$(VM_SUBDIR)
			J9VM_PATH=$(TEST_JRE_BIN)$(D)j9vm
		else
			JAVA_SHARED_LIBRARIES_DIR:=$(TEST_JRE_LIB_DIR)$(D)$(ARCH_DIR)$(D)$(VM_SUBDIR)
			J9VM_PATH=$(TEST_JRE_LIB_DIR)$(D)$(ARCH_DIR)$(D)j9vm
		endif
		ADD_JVM_LIB_DIR_TO_LIBPATH:=export LIBPATH=$(Q)$(LIBPATH)$(P)$(TEST_JRE_LIB_DIR)$(D)$(VM_SUBDIR)$(P)$(JAVA_SHARED_LIBRARIES_DIR)$(P)$(TEST_JRE_BIN)$(D)j9vm$(Q);
	else
		ifneq (,$(findstring win,$(SPEC)))
			JAVA_SHARED_LIBRARIES_DIR:=$(TEST_JDK_BIN)$(D)$(VM_SUBDIR)
			J9VM_PATH=$(TEST_JDK_BIN)$(D)j9vm
		else
			JAVA_SHARED_LIBRARIES_DIR:=$(TEST_JDK_LIB_DIR)$(D)$(ARCH_DIR)$(D)$(VM_SUBDIR)
			J9VM_PATH=$(TEST_JDK_LIB_DIR)$(D)$(ARCH_DIR)$(D)j9vm
		endif
		ADD_JVM_LIB_DIR_TO_LIBPATH:=export LIBPATH=$(Q)$(LIBPATH)$(P)$(TEST_JDK_LIB_DIR)$(D)$(VM_SUBDIR)$(P)$(JAVA_SHARED_LIBRARIES_DIR)$(P)$(TEST_JDK_BIN)$(D)j9vm$(Q);
	endif
else
	ifeq (8, $(JDK_VERSION))
		ifneq (,$(findstring win,$(SPEC)))
			VM_SUBDIR_PATH=$(TEST_JRE_BIN)$(D)$(VM_SUBDIR)
			J9VM_PATH=$(TEST_JRE_BIN)$(D)j9vm
		else
			VM_SUBDIR_PATH=$(TEST_JRE_LIB_DIR)$(D)$(ARCH_DIR)$(D)$(VM_SUBDIR)
			J9VM_PATH=$(TEST_JRE_LIB_DIR)$(D)$(ARCH_DIR)$(D)j9vm
			LIB_PATH=$(TEST_JRE_LIB_DIR)$(D)$(ARCH_DIR)
		endif
	else
		ifneq (,$(findstring win,$(SPEC)))
			VM_SUBDIR_PATH=$(TEST_JDK_BIN)$(D)$(VM_SUBDIR)
			J9VM_PATH=$(TEST_JDK_BIN)$(D)j9vm
		else
			VM_SUBDIR_PATH=$(TEST_JDK_LIB_DIR)$(D)$(VM_SUBDIR)
			J9VM_PATH=$(TEST_JDK_LIB_DIR)$(D)j9vm
			LIB_PATH=$(TEST_JDK_LIB_DIR)
		endif
	endif

	PS=:
	ifneq (,$(findstring win,$(SPEC)))
		ifeq ($(CYGWIN),1)
			NATIVE_TEST_LIBS := $(shell cygpath -u $(NATIVE_TEST_LIBS))
			VM_SUBDIR_PATH := $(shell cygpath -u $(VM_SUBDIR_PATH))
			J9VM_PATH := $(shell cygpath -u $(J9VM_PATH))
		else
			PS=;
		endif
	endif

	TEST_LIB_PATH_VALUE:=$(NATIVE_TEST_LIBS)$(PS)$(VM_SUBDIR_PATH)$(PS)$(J9VM_PATH)

	ifneq (,$(findstring win,$(SPEC)))
		TEST_LIB_PATH:=PATH=$(Q)$(TEST_LIB_PATH_VALUE)$(PS)$(PATH)$(Q)
	else ifneq (,$(findstring aix,$(SPEC)))
		TEST_LIB_PATH:=LIBPATH=$(Q)$(TEST_LIB_PATH_VALUE)$(PS)$(LIBPATH)$(Q)
	else ifneq (,$(findstring zos,$(SPEC)))
		TEST_LIB_PATH:=LIBPATH=$(Q)$(TEST_LIB_PATH_VALUE)$(PS)$(LIB_PATH)$(PS)$(LIBPATH)$(Q)
	else ifneq (,$(findstring osx,$(SPEC)))
		TEST_LIB_PATH:=DYLD_LIBRARY_PATH=$(Q)$(TEST_LIB_PATH_VALUE)$(PS)$(DYLD_LIBRARY_PATH)$(Q)
	else
		TEST_LIB_PATH:=LD_LIBRARY_PATH=$(Q)$(TEST_LIB_PATH_VALUE)$(PS)$(LD_LIBRARY_PATH)$(Q)
	endif

	JAVA_SHARED_LIBRARIES_DIR:=$(NATIVE_TEST_LIBS)
	ADD_JVM_LIB_DIR_TO_LIBPATH:=export $(TEST_LIB_PATH);
endif

ifneq ($(DEBUG),)
$(info JAVA_SHARED_LIBRARIES_DIR is set to $(JAVA_SHARED_LIBRARIES_DIR))
$(info VM_SUBDIR is set to $(VM_SUBDIR))
$(info ADD_JVM_LIB_DIR_TO_LIBPATH is set to $(ADD_JVM_LIB_DIR_TO_LIBPATH))
endif

#######################################
# TEST_STATUS
# if TEST_RESULTSTORE is enabled, failed test will uploaded to vmfarm result store
#######################################
ifneq ($(TEST_RESULTSTORE),)
ifndef BUILD_ID
$(error BUILD_ID is needed for uploading files to result store)
endif
ifndef JOB_ID
$(error JOB_ID is needed for uploading files to result store)
endif
ifndef JAVATEST_ROOT
$(error JAVATEST_ROOT is needed for uploading files to result store)
endif
ifneq (,$(findstring aix,$(PLATFORM)))
AXXONRESULTSSERVER=filesystem:/cores/tmp
else
AXXONRESULTSSERVER=vmfarm.rtp.raleigh.ibm.com:31
endif
TEST_STATUS=if [ $$? -eq 0 ] ; then $(ECHO) $(Q)$(Q); $(ECHO) $(Q)$@$(Q)$(Q)_PASSED$(Q); $(ECHO) $(Q)$(Q); $(CD) $(TEST_ROOT); $(RM_REPORTDIR) else perl $(Q)-I$(JAVATEST_ROOT)$(D)lib$(D)perl$(Q) -mResultStore::Uploader -e $(Q)ResultStore::Uploader::upload('.',$(BUILD_ID),$(JOB_ID),'$(AXXONRESULTSSERVER)','results-$(JOB_ID)')$(Q); $(ECHO) $(Q)$(Q); $(ECHO) $(Q)$@$(Q)$(Q)_FAILED$(Q); $(ECHO) $(Q)$(Q); fi
endif

#######################################
# CONVERT_TO_EBCDIC_CMD
# convert ascii to ebcdic on zos
#######################################
TOEBCDIC_CMD= \
$(ECHO) $(Q)converting ascii files to ebcdic$(Q); \
find | grep \\.java$ | while read f; do echo $$f; iconv -f iso8859-1 -t ibm-1047 < $$f > $$f.ebcdic; rm $$f; mv $$f.ebcdic $$f; done; \
find | grep \\.txt$ | while read f; do echo $$f; iconv -f iso8859-1 -t ibm-1047 < $$f > $$f.ebcdic; rm $$f; mv $$f.ebcdic $$f; done; \
find | grep \\.mf$ | while read f; do echo $$f; iconv -f iso8859-1 -t ibm-1047 < $$f > $$f.ebcdic; rm $$f; mv $$f.ebcdic $$f; done; \
find | grep \\.policy$ | while read f; do echo $$f; iconv -f iso8859-1 -t ibm-1047 < $$f > $$f.ebcdic; rm $$f; mv $$f.ebcdic $$f; done; \
find | grep \\.props$ | while read f; do echo $$f; iconv -f iso8859-1 -t ibm-1047 < $$f > $$f.ebcdic; rm $$f; mv $$f.ebcdic $$f; done; \
find | grep \\.sh$ | while read f; do echo $$f; iconv -f iso8859-1 -t ibm-1047 < $$f > $$f.ebcdic; rm $$f; mv $$f.ebcdic $$f; done; \
find | grep \\.pl$ | while read f; do echo $$f; iconv -f iso8859-1 -t ibm-1047 < $$f > $$f.ebcdic; rm $$f; mv $$f.ebcdic $$f; done; \
find | grep \\.xml$ | while read f; do echo $$f; iconv -f iso8859-1 -t ibm-1047 < $$f > $$f.ebcdic; rm $$f; mv $$f.ebcdic $$f; done;

CONVERT_TO_EBCDIC_CMD=
ifneq (,$(findstring zos,$(SPEC)))
	CONVERT_TO_EBCDIC_CMD=$(TOEBCDIC_CMD)
endif
