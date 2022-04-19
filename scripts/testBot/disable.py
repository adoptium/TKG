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

import playlistModifier
import argparse
import re
import sys
import shlex

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-m", "--message", required=True, help="exclude message")
    ap.add_argument("-d", "--directory", required=True, help="directory to modify")
    ap.add_argument("-c", "--comment", required=True, help="comment")
    ap.add_argument("-cp", "--copyright", required=False, default="false", help="update copyright date")
    args = vars(ap.parse_args())

    newArgs = {
        "directory": args["directory"],
        "copyright": args["copyright"],
        "comment": args["comment"]
    }
    message = args["message"].strip()
    match = re.match(r"auto ([A-Za-z]+) test (.*)", message)

    if match:
        print(f"{args['message']}\n")
        statementlist = match.group(2).split(";")
        tests = []
        for statement in statementlist:
            words = statement.strip().split(" ")
            test = {}
            for word in words:
                if "=" not in word:
                    test["name"] = word
                elif m := re.match(r"impl=(.*)", word):
                    test["impl"] = m.group(1)
                elif m := re.match(r"vendor=(.*)", word):
                    test["vendor"] = m.group(1)
                elif m := re.match(r"ver=(.*)", word):
                    test["ver"] = m.group(1)
                elif m := re.match(r"plat=(.*)", word):
                    test["plat"] = m.group(1)
                elif m := re.match(r"testflag=(.*)", word):
                    test["testflag"] = m.group(1)
                else:
                    print(f"unrecognized argument: {word}")
                    sys.exit(-1)
            tests.append(test)
        newArgs.update({
            "mode": match.group(1),
            "tests": tests,
        })
        playlistModifier.run(newArgs)

    else:
        print(f"unrecognized message: {args['message']}")
        sys.exit(-1)

if __name__ == "__main__":
    main() 