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
    match = re.match(r"auto ([A-Za-z]+) test (\S+)( in (\S+))?", message)
    if match:
        print(f"{args['message']}\n")
        file = "playlist.xml"
        if match.group(3):
            file = match.group(4)
        if (file == "playlist.xml"):
            newArgs.update({
                "mode": match.group(1),
                "test": match.group(2),
                "comment": args["comment"],
                "file": file
            })
            playlistModifier.run(newArgs)
        else:
            print("function not supported for now")
    else:
        print(f"unrecognized message: {args['message']}")

if __name__ == "__main__":
    main()
