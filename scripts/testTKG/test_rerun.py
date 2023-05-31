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

# test rerun tag
def run_test():
    rt = True
    printTestheader("rerun")

    rt &= test_rerun_helper("all", ["testIgnoreOnRerun_0", "testIgnoreOnRerunSubDir_0", "testIgnoreOnRerunSubDir_1"], ["testRerun_0", "testDefault_0", "testIgnoreOnRerun_0", "testRerunSubDir_0", "testDefaultSubDir_0", "testIgnoreOnRerunSubDir_0", "testIgnoreOnRerunSubDir_1"])
    rt &= test_rerun_helper("system", ["testIgnoreOnRerunSubDir_0", "testIgnoreOnRerunSubDir_1"], ["testIgnoreOnRerunSubDir_0", "testIgnoreOnRerunSubDir_1"])
    rt &= test_rerun_helper("perf", [""], set())

    if not rt:
        return False
    else:
        print("\tTest successful!")
        return True

# run target and validate IGNORE_ON_RERUN in the rerun.mk are as expected
def test_rerun_helper(target, expected_ignore_on_rerun_list, expected_success):
    buildList = "TKG/examples/rerun"
    command = f"make _{target}" 
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)

    rt = checkResult(result, expected_success, set(), set(), set())

    if not rt:
        return False

    regex = re.compile(r"IGNORE_ON_RERUN=(.*)")
    utils_file = "../../rerun.mk"
    ignore_on_rerun_tests = ""
    with open(utils_file) as f:
        for line in f:
            result = regex.search(line)
            if result is not None:
                ignore_on_rerun_tests = result.group(1)

    ignore_on_rerun_list = ignore_on_rerun_tests.split(",")

    if set(expected_ignore_on_rerun_list) == set(ignore_on_rerun_list):
        print("\tIgnore on rerun list check successful!")
        return True
    else:
        printError("\tIgnore on rerun list check failed!")
        printError(f"expected ignore on rerun list: {expected_ignore_on_rerun_list}; actual list:{ignore_on_rerun_list}")
        return False

if __name__ == "__main__":
    run_test()