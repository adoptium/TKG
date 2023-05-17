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

import re
import textwrap

CD_TKG = "cd ../../"
MAKE_CLEAN = "make clean"
MAKE_COMPILE = "make compile"
DYNAMIC_COMPILE = "export DYNAMIC_COMPILE=true"
EXPORT_BUILDLIST = "export BUILD_LIST"
log_level = "default"

def setVerboseLevel(level):
    global log_level
    log_level = level

def printTestheader(msg):
    print(f"---------------------------------\nRun test: {msg}")

def printError(msg):
    print(f"\tError: {msg}")

def checkTestNum(group, expected, result):
    if expected != result:
        printError(f"expected {group} number: {expected}; actual number: {result} ")
        return False
    return True

def checkTestDetail(group, detailBlock, expected):
    rt = True
    result = re.search(f"{group} test targets:\n(.*?)\n\n", detailBlock, flags=re.DOTALL)
    if result is not None:
        tests = result.group(1).split()
        if len(tests) != len(expected):
            rt = False
        for test in tests:
            if test not in expected:
                rt = False
                break
        if not rt:
            printError(f"expected {group} list: {expected}; actual list:{tests}")
    return rt

def checkResult(result, passed, failed, disabled, skipped):
    global verbose
    rt = True
    stdout = result.stdout.decode()
    stderr = result.stderr.decode()
    if len(failed) == 0 ^ result.returncode != 0:
        printError(f"test exit code: {result.returncode} is not expected")

    summary = re.search(r"TEST TARGETS SUMMARY\n\+{74}\n(.*)TOTAL: (\d+)   EXECUTED: (\d+)   PASSED: (\d+)   FAILED: (\d+)   DISABLED: (\d+)   SKIPPED: (\d+)\n(.*)\+{74}", stdout, flags=re.DOTALL)

    if log_level == "verbose":
        stdout = textwrap.indent(stdout, '\t')
        stderr = textwrap.indent(stderr, '\t')
        print(f"\tstdout:\n{stdout}\n\n")
        print(f"\tstderr:\n{stderr}\n\n")

    if summary is None:
        printError("test not completed")
        return False

#    print(summary.group())
    testDetail = summary.group(1)
    totalNum = int(summary.group(2))
    exectuedNum = int(summary.group(3))
    passedNum = int(summary.group(4))
    failedNum = int(summary.group(5))
    disabledNum = int(summary.group(6))
    skippedNum = int(summary.group(7))
    rt &= checkTestNum('PASSED', len(passed), passedNum)
    rt &= checkTestDetail("PASSED", testDetail, passed)
    rt &= checkTestNum('FAILED', len(failed), failedNum)
    rt &= checkTestDetail("FAILED", testDetail, failed)
    rt &= checkTestNum('DISABLED', len(disabled), disabledNum)
    rt &= checkTestDetail("DISABLED", testDetail, disabled)
    rt &= checkTestNum('SKIPPED', len(skipped), skippedNum)
    rt &= checkTestNum('TOTAL', len(passed) + len(failed) + len(disabled) + len(skipped), totalNum)
    rt &= checkTestNum('EXECUTED', len(passed) + len(failed), exectuedNum)

    if not rt:
        print("\tTest failed!")
        return False
    else:
        print("\tTest successful!")
        return True