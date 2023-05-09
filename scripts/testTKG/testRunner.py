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
import sys
import os

from utils import *
from os import listdir
from os.path import isfile, join

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('-t','--test', nargs='+', required=False, default=[], help="space separated test list")
    ap.add_argument('-v','--verbose', nargs='?', required=False, const=True, default=False, help="print test output")
    args = vars(ap.parse_args())
    test_list = args['test']

    if not test_list:
        test_files = [f for f in listdir(os.getcwd()) if isfile(join(os.getcwd(), f)) and f.startswith("test_")]
        for file in test_files:
            test_file_strs = file.split(".")
            test_list.append(test_file_strs[0])

    if args['verbose']:
        setVerboseLevel("verbose")

    rt = True

    for test in test_list:
        __import__(test)
        test_module = sys.modules[test]
        rt &= test_module.run_test()

    if rt:
        print("ALL TESTS PASSED")
    else:
        print("FAILED")
        exit(1)

if __name__ == "__main__":
    main()
