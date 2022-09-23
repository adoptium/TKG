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

ifndef TKG_ITERATIONS
	export TKG_ITERATIONS=1
endif

ifneq (,$(findstring AOT,$(TEST_FLAG)))
	export TR_silentEnv=1
	export TR_Options=forceAOT
	export TR_OptionsAOT=forceAOT
	AOT_OPTIONS = -Xshareclasses:name=test_aot -Xscmx400M -Xscmaxaot256m
	ifndef AOT_ITERATIONS
		export AOT_ITERATIONS=2
	endif
endif
