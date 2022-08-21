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

import argparse
import subprocess
import re
import textwrap

CD_TKG = "cd ../../"
MAKE_CLEAN = "make clean"
MAKE_COMPILE = "make compile"
DYNAMIC_COMPILE = "export DYNAMIC_COMPILE=true"
EXPORT_BUILDLIST = "export BUILD_LIST"
ALL_TESTS = ["base","level","hierarchy","platformRequirements"]

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('-l','--testList', nargs='+', required=False, default=ALL_TESTS, help="space separated test list")
    args = vars(ap.parse_args())
    testList = args['testList']

    rt = True

    for test in testList:
        if f"test_{test}" in globals():
            rt &= globals()[f"test_{test}"]()

    if rt:
        print("ALL TESTS PASSED")
    else:
        print("FAILED")
        exit(1)

def test_base():
    rt = True
    printTestheader("base")

    buildList = "TKG/examples/base"
    command = "make _all"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt = checkResult(result, {'testSuccess_0'}, {'testFail_0'}, set(), set())

    command = "make _testSuccess"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt = checkResult(result, {'testSuccess_0'}, set(), set(), set())
    return rt

def test_level():
    rt = True
    printTestheader("level")

    buildList = "TKG/examples/sliceAndDice/level"

    command = "make _sanity"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt = checkResult(result, {'testSanity_0'}, set(), set(), set())

    command = "make _extended"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt = checkResult(result, {'testDefault_0', 'testExtended_0'}, set(), set(), set())

    command = "make _dev"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt = checkResult(result, {'testDev_0'}, set(), set(), set())
    
    command = "make _special"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt = checkResult(result, {'testSpecial_0'}, set(), set(), set())

    command = "make _all"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)
    rt = checkResult(result, {'testSanity_0', 'testDefault_0', 'testExtended_0', 'testDev_0', 'testSpecial_0'}, set(), set(), set())
    return rt

def test_dynamic_compile():
    print("dynamic compile")

def test_hierarchy():
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

def test_platformRequirements():
    rt = True
    printTestheader("platformRequirements")

    buildList = "TKG/examples/platformRequirements"
    command = "make _all"
    print(f"\t{command}")
    result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)

    stdout = result.stdout.decode()
    specStr = re.search(r"set SPEC to (.*)", stdout)

    if specStr is not None:
        spec = specStr.group(1)
    else:
        printError("Could not parse spec from output.")
        return False

    passed = set()
    skipped = set()

    if 'x86' in spec:
        passed.add('test_arch_x86_0')
        skipped.add('test_arch_nonx86_0')
    else:
        passed.add('test_arch_nonx86_0')
        skipped.add('test_arch_x86_0')

    if '64' in spec:
        passed.add('test_bits_64_0')
    else:
        skipped.add('test_bits_64_0')

    if 'osx' in spec:
        passed.add('test_os_osx_0')
    else:
        skipped.add('test_os_osx_0')

    if 'osx_x86-64' == spec:
        passed.add('test_osx_x86-64_0')
    else:
        skipped.add('test_osx_x86-64_0')

    if 'x86' in spec or '390' in spec:
        passed.add('test_arch_x86_390_0')
    else:
        skipped.add('test_arch_x86_390_0')

    if 'osx_x86-64' == spec or 'win_x86' == spec or 'aix_ppc-64' == spec:
        passed.add('test_osx_x86-64_win_x86_aix_ppc-64_0')
    else:
        skipped.add('test_osx_x86-64_win_x86_aix_ppc-64_0')

    rt &= checkResult(result, passed, set(), set(), skipped)
    return rt

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
    rt = True
    stdout = result.stdout.decode()
    stderr = result.stderr.decode()
    if len(failed) == 0 ^ result.returncode != 0:
        printError(f"test exit code: {result.returncode} is not expected")

    summary = re.search(r"TEST TARGETS SUMMARY\n\+{74}\n(.*)TOTAL: (\d+)   EXECUTED: (\d+)   PASSED: (\d+)   FAILED: (\d+)   DISABLED: (\d+)   SKIPPED: (\d+)\n(.*)\+{74}", stdout, flags=re.DOTALL)
    if summary is None:
        printError("test not completed")
        stdout = textwrap.indent(stdout, '\t')
        stderr = textwrap.indent(stderr, '\t')
        print(f"\tstdout:\n{stdout}\n\n")
        print(f"\tstderr:\n{stderr}\n\n")
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

if __name__ == "__main__":
    main()
