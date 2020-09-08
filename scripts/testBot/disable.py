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

import autoModifier
import argparse
import re
import sys

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-m", "--message", required=True, help="exclude message")
    ap.add_argument("-d", "--directory", required=True, help="directory to modify")
    ap.add_argument("-i", "--issue", required=True, help="issue URL")
    args = vars(ap.parse_args())

    newArgs = {
        "directory": args["directory"]
    }
    message = args["message"].strip()
    match = re.match(r"auto ([A-Za-z]+) test (\S+)( in (\S+))?", message)
    if match:
        print(f"{args['message']}\n")
        mode = match.group(1)
        test = match.group(2)
        file = "playlist.xml"
        if match.group(3):
            file = match.group(4)
        newArgs.update({
            "file": file
        })
        if (mode == "exclude") and (file == "playlist.xml"):
            excludeTestInPlaylist(test, args["issue"], newArgs)
        else:
            print("function not supported for now")
    else:
        print(f"unrecognized message: {args['message']}")


def excludeTestInPlaylist(test, issue, args):
    args.update({
        "find" : fr"(([ \t]*)<\s*testCaseName\s*>\s*{test}\s*<\s*/testCaseName\s*>)",
        "replace": fr"\1\n\2<disabled>{issue}</disabled>"
    })
    rt = autoModifier.run(args)
    if not rt:
        print(f"Could not find {test} in {args['file']}!")
        sys.exit(-1)


if __name__ == "__main__":
    main()
