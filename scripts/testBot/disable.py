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
    args = vars(ap.parse_args())

    newArgs = {
        "directory": args["directory"]
    }
    message = args["message"].strip()
    match = re.match(r"auto ([A-Za-z]+) test (\S+)(.*)", message)
    if match:
        print(f"{args['message']}\n")
        if match.group(3):
            options = shlex.split(match.group(3))
            for op in options:
                if m := re.match(r"file=(.*)", op):
                    newArgs["file"] = m.group(1)
                elif m := re.match(r"impl=(.*)", op):
                    newArgs["impl"] = m.group(1)
                elif m := re.match(r"vendor=(.*)", op):
                    newArgs["vendor"] = m.group(1)
                elif m := re.match(r"ver=(.*)", op):
                    newArgs["ver"] = m.group(1)
                elif m := re.match(r"plat=(.*)", op):
                    newArgs["plat"] = m.group(1)
                else:
                    print(f"unrecognized argument: {op}")
                    sys.exit(-1)
        if ("file" not in newArgs):
            newArgs["file"] = "playlist.xml"
        if (newArgs["file"] == "playlist.xml"):
            newArgs.update({
                "mode": match.group(1),
                "test": match.group(2),
                "comment": args["comment"]
            })
            playlistModifier.run(newArgs)
        else:
            print("function not supported for now")
    else:
        print(f"unrecognized message: {args['message']}")
        sys.exit(-1)

if __name__ == "__main__":
    main() 