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
    printTestheader("jdkVersion")

    buildList = "TKG/examples/jdkVersion"
    command = "make _all"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)

    stdout = result.stdout.decode()
    verStr = re.search(r"set JDK_VERSION to (.*)", stdout)

    if verStr is not None:
        try:
            ver = int(verStr.group(1))
        except ValueError:
            print("\tTest Skipped! JDK_VERSION is not integer.")
            return True
    else:
        printError("Could not parse JDK_VERSION from output.")
        return False

    passed = set()
    disabled = set()

    if 8 <= ver < 11:
        passed.add('test_ver_8to11_0')
        passed.add('test_ver_8to11and17plus_0')
        passed.add('test_disable_ver_11_0')
        passed.add('test_disable_ver_11to17_0')
        disabled.add('test_disable_ver_8and17plus_0')
    elif ver == 11:
        passed.add('test_ver_11_0')
        passed.add('test_ver_8to11_0')
        passed.add('test_ver_8to11and17plus_0')
        disabled.add('test_disable_ver_11_0')
        disabled.add('test_disable_ver_11to17_0')
        passed.add('test_disable_ver_8and17plus_0')
    elif ver >= 17:
        passed.add('test_ver_17plus_0')
        passed.add('test_ver_8to11and17plus_0')
        passed.add('test_disable_ver_11_0')
        disabled.add('test_disable_ver_11to17_0')
        disabled.add('test_disable_ver_8and17plus_0')

    rt &= checkResult(result, passed, set(), disabled, set())
    return rt

if __name__ == "__main__":
    run_test()