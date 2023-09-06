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
    printTestheader("feature")

    buildList = "TKG/examples/feature"
    command = "make _all"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testFeatureFips_0', 'testFeatureFipsNoApplicable_0'}, set(), set(), set())

    buildList = "TKG/examples/feature"
    command = "make _all"
    testFlag = "export TEST_FLAG=FIPS140_2"
    print(f"\t{testFlag}; {command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {testFlag}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testFeatureFips_0'}, set(), set(), set())

    command = "make _all"
    testFlag = "export TEST_FLAG=FIPS140_3_20220501"
    print(f"\t{testFlag}; {command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {testFlag}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testFeatureFips_0', 'testFeatureFipsNoApplicable_0'}, set(), set(), set())

    command = "make _all"
    testFlag = "export TEST_FLAG=FIPS140_3_20230511"
    print(f"\t{testFlag}; {command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {testFlag}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testFeatureFips_0', 'testFeatureFipsNoApplicable_0', 'testFeatureFipsRegexRequired_0'}, set(), set(), set())

    command = "make _all"
    testFlag = "export TEST_FLAG=FIPS140_3_20220101"
    print(f"\t{testFlag}; {command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {testFlag}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'testFeatureFips_0', 'testFeatureFipsNoApplicable_0', 'testFeatureFipsRequired_0'}, set(), set(), set())

    return rt

if __name__ == "__main__":
    run_test()