################################################################################
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
################################################################################

import subprocess
from utils import *

def run_test():
    rt = True
    printTestheader("level")

    buildList = "TKG/examples/sliceAndDice/level"

    command = "make _sanity"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testSanity_0'}, set(), set(), set())

    command = "make _extended"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testDefault_0', 'testExtended_0'}, set(), set(), set())

    command = "make _dev"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testDev_0'}, set(), set(), set())
    
    command = "make _special"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testSpecial_0'}, set(), set(), set())

    command = "make _all"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testSanity_0', 'testDefault_0', 'testExtended_0', 'testDev_0', 'testSpecial_0'}, set(), set(), set())
    return rt

if __name__ == "__main__":
    run_test()