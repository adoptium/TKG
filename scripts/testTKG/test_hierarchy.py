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
    printTestheader("hierarchy")

    buildList = "TKG/examples/hierarchy"
    command = "make _all"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'test_a1_b1_0', 'test_a2_0', 'test_a2_b2_0', 'test_a2_b2_c2_0', 'test_a2_d2_0', 'test_a3_b3_0', 'test_a3_b3_c3_d3_0', 'test_a3_f3_0'}, set(), set(), set())

    command = "make _test_a3_b3_c3_d3_0"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'test_a3_b3_c3_d3_0'}, set(), set(), set())

    buildList = "TKG/examples/hierarchy/a2/b2,TKG/examples/hierarchy/a3/f3"
    command = "make _all"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt &= checkResult(result, {'test_a2_b2_0', 'test_a2_b2_c2_0', 'test_a3_f3_0'}, set(), set(), set())

    return rt

if __name__ == "__main__":
    run_test()