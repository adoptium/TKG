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
# Makefile to run various JVM tests
#

.DEFAULT_GOAL := test

D = /

SUBDIRS = ..

ifndef TEST_ROOT
	TEST_ROOT := $(shell pwd)$(D)..
endif

include settings.mk

include count.mk

runtest:
	@$(MAKE) -C $(TEST_ROOT) -f autoGen.mk _all

.PHONY: runtest

test: compile runtest
	@$(ECHO) "All Tests Completed"

.PHONY: test

.NOTPARALLEL: test

failed:
	@$(MAKE) -f failedtargets.mk failed

.PHONY: failed

.NOTPARALLEL: failed
