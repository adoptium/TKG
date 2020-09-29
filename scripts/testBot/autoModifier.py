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
import os
import argparse
from pathlib import Path

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-f", "--file", required=True, help="regex file name to modify")
    ap.add_argument("-s", "--find", required=True, help="regex string to search in file")
    ap.add_argument("-r", "--replace", required=True, help="regex string to replace found")
    ap.add_argument("-d", "--directory", required=True, help="directory to modify")
    run(vars(ap.parse_args()))


def run(args):
    print(f"- searching file {args['file']} in {args['directory']}")
    files = find_files(args["file"], args['directory'])
    print(f"- updating file(s)")
    isModified = modifyFiles(files, args["find"], args["replace"])
    return isModified


def find_files(name, path):
    result = []
    for root, dirs, files in os.walk(path):
        for filename in files:
            if re.match(name, filename):
                result.append(os.path.join(root, filename))
                print(".", end="")
    print("\n")
    return result


def modifyFiles(files, find, replace):
    isModified = False
    for file in files:
        pattern = re.compile(find)
        with open(file) as inFile:
            input = inFile.read()
            search = re.search(find, input)
            if search is not None:
                output = re.sub(find, replace, input)
                temp = file + ".temp"
                with open(temp, "w") as outFile:
                    outFile.write(output)
                    os.remove(file)
                    os.rename(temp, file)
                    print(f"updated {file}\n")
                    isModified = True
    return isModified


if __name__ == "__main__":
    main()
