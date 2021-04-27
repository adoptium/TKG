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

.DEFAULT_GOAL := clean

include common.mk

ifndef TEST_ROOT
	TEST_ROOT := $(shell pwd)$(D)..
endif

include settings.mk

cleanBuild:
	$(RM) -r $(BUILD_ROOT)

clean: cleanBuild
	$(RM) -r $(TEST_ROOT)$(D)TKG$(D)output_*
	$(RM) $(FAILEDTARGETS)
	ant -f $(TEST_ROOT)$(D)TKG$(D)scripts/build_tools.xml clean

.PHONY: cleanBuild clean
