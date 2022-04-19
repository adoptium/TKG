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

import sys
import copy
import re
import os
import argparse
from pathlib import Path
import lxml.etree as etree
import datetime

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-d", "--directory", required=True, help="directory to modify")
    ap.add_argument("-m", "--mode", required=True, help="operation mode, e.g., exclude")
    ap.add_argument("-t", "--tests", required=False, help="exclude test list")
    ap.add_argument("-c", "--comment", required=False, help="comment")
    ap.add_argument("-cp", "--copyright", required=False, default="false", help="update copyright date")
    run(vars(ap.parse_args()))

def run(args):
    print(f"- searching file playlist.xml in {args['directory']}")
    files = find_files("playlist.xml", args['directory'])
    if not files:
        print(f"Could not find file {args['file']}!")
        sys.exit(-1)
    print(f"- updating file(s)")
    if (args["mode"] == "format"):
        formatter(files, args)
    if (args["mode"] == "exclude"):
        for test in args["tests"]:
            updated = addDisabled(files, test, args)
            if not updated:
                print(f"Could not find test {test['name']}!")
                sys.exit(-1)

def find_files(name, path):
    result = []
    for root, dirs, files in os.walk(path):
        for filename in files:
            if re.match(rf"^{name}$", filename):
                result.append(os.path.join(root, filename))
                print(".", end="")
    print("\n")
    return result

def addDisabled(files, test, args):
    updated = False
    testCaseName = test["name"]
    match = re.match(r"^(\S+)_(\d+)$", testCaseName)
    nthVar = None
    if match:
        testCaseName = match.group(1)
        nthVar = int(match.group(2))
    for file in files:
        root = etree.parse(file)
        testEle = root.xpath(f"./test/testCaseName[text()='{testCaseName}']/..")
        if testEle:
            disable = etree.Element("disable")
            commentEle = etree.Element("comment")
            commentEle.text = args["comment"]
            disable.append(commentEle)
            if nthVar is not None:
                var = testEle[0].find("variations")
                if var is not None and nthVar < len(var):
                    disable.append(copy.deepcopy(var[nthVar]))
                elif nthVar == 0:
                    varEle = etree.Element("variation")
                    varEle.text = "NoOptions"
                    disable.append(varEle)
                else:
                    print(f"Could not find test case {testCaseName}_{nthVar} (i.e., the {nthVar + 1}th variation for test {testCaseName})!")
                    sys.exit(-1)
            if "ver" in test:
                verEle = etree.Element("version")
                verEle.text = test["ver"]
                disable.append(verEle)
            if "impl" in test:
                implEle = etree.Element("impl")
                implEle.text = test["impl"]
                disable.append(implEle)
            if "vendor" in test:
                vendorEle = etree.Element("vendor")
                vendorEle.text = test["vendor"]
                disable.append(vendorEle)
            if "plat" in test:
                platEle = etree.Element("platform")
                platEle.text = test["plat"]
                disable.append(platEle)
            if "testflag" in test:
                testflagEle = etree.Element("testflag")
                testflagEle.text = test["testflag"]
                disable.append(testflagEle)
            disables = testEle[0].find("disables")
            if disables is None:
                disables = etree.Element("disables")
            disables.append(disable)
            testCaseName = testEle[0].find("testCaseName")
            testCaseName.addnext(disables)
            updateCopyright(root, args)
            updateFile(file, root)
            updated = True
    return updated

def formatter(files, args):
    for file in files:
        root = etree.parse(file)
        updateCopyright(root, args)
        updateFile(file, root)

def updateCopyright(root, args):
    if (args["copyright"] == "true"):
        cr = root.xpath(f"/comment()")
        if cr:
            crtext = cr[0].text
            find = fr"(.*)(Copyright \(c\) \d{{4}}, )(\d{{4}})(.*)"
            search = re.search(find, crtext)
            year = datetime.datetime.now().year
            if (search is not None) and (search.group(3) != str(year)):
                replace = fr"\g<1>\g<2>{year}\g<4>"
                newcrtext = re.sub(find, replace, crtext, flags=re.DOTALL)
                cr[0].text = newcrtext

def updateFile(file, root):
    etree.indent(root, "\t")
    temp = file + ".temp"
    with open(temp, 'wb') as outfile:
        root.write(outfile, pretty_print=True, xml_declaration=True, encoding="utf-8")
        os.remove(file)
        os.rename(temp, file)
        print(f"updated {file}\n")

if __name__ == "__main__":
    main()
