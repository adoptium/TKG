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
# Makefile to compare auto detect values with setting values
#

-include autoGenEnv.mk

HOTSPOT_IMPLS=$(or hotspot,sap)

ifneq ($(AUTO_DETECT), false) 
    export SPEC:=$(DETECTED_SPEC)

    export MICROARCH:=$(DETECTED_MICRO_ARCH)

    ifndef JDK_VERSION
        export JDK_VERSION:=$(DETECTED_JDK_VERSION)
    else
        ifneq ($(JDK_VERSION), $(DETECTED_JDK_VERSION))
            $(error DETECTED_JDK_VERSION value is $(DETECTED_JDK_VERSION), settled JDK_VERSION value is $(JDK_VERSION). JDK_VERSION value does not match. Please reset or unset JDK_VERSION)
        endif
    endif

    ifndef JDK_IMPL
        export JDK_IMPL:=$(DETECTED_JDK_IMPL)
    else
        ifneq ($(JDK_IMPL), $(DETECTED_JDK_IMPL))
            ifneq ($(JDK_IMPL), sap)
                $(error DETECTED_JDK_IMPL value is $(DETECTED_JDK_IMPL), settled JDK_IMPL value is $(JDK_IMPL). JDK_IMPL value does not match. Please reset or unset JDK_IMPL)
            endif
        endif
    endif

    ifndef JDK_VENDOR
        export JDK_VENDOR:=$(DETECTED_JDK_VENDOR)
    else
        ifneq ($(JDK_VENDOR), $(DETECTED_JDK_VENDOR))
            $(error DETECTED_JDK_VENDOR value is $(DETECTED_JDK_VENDOR), settled JDK_VENDOR value is $(JDK_VENDOR). JDK_VENDOR value does not match. Please reset or unset JDK_VENDOR)
        endif
    endif

    export TEST_FLAG:=$(DETECTED_TEST_FLAG)

else
    $(info AUTO_DETECT is set to false)
    ifndef SPEC
        export SPEC:=$(DETECTED_SPEC)
        $(info Warning: No SPEC has been exported. Use auto detected SPEC=$(SPEC).)
    endif
    ifndef MICROARCH
        export MICROARCH:=$(DETECTED_MICROARCH)
        $(info Warning: No MICROARCH has been exported. Use auto detected MICROARCH=$(MICROARCH).)
    endif
    ifndef JDK_VERSION
        export JDK_VERSION:=$(DETECTED_JDK_VERSION)
        $(info Warning: No JDK_VERSION has been exported. Use auto detected JDK_VERSION=$(JDK_VERSION).)
    endif
    ifndef JDK_IMPL
        export JDK_IMPL:=$(DETECTED_JDK_IMPL)
        $(info Warning: No JDK_IMPL has been exported. Use auto detected JDK_IMPL=$(JDK_IMPL).)
    endif
    ifndef JDK_VENDOR
        export JDK_VENDOR:=$(DETECTED_JDK_VENDOR)
        $(info Warning: No JDK_VENDOR has been exported. Use auto detected JDK_VENDOR=$(JDK_VENDOR).)
    endif
endif

export MALLOCOPTIONS=