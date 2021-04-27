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

from sys import argv
import os
import glob

if len(argv) != 2:
    print("Usage: count-java-tests.py [directory]")
    exit()

searchDir = argv[1]
if not os.path.isdir(searchDir):
    print("Error! '" + searchDir + "' is not a directory")
    exit()

print("\nCounting tests in '" + searchDir + "' ...")


def get_filepaths(directory, extension):
    file_paths = []
    for root, directories, files in os.walk(directory):
        for filename in files:
            if filename.endswith(extension):
                filepath = os.path.join(root, filename)
                file_paths.append((root, filepath, filename))
    return file_paths


# Run the above function and store its results in a variable.   
full_file_paths = get_filepaths(searchDir, "java")

print("\nFound " + str(len(full_file_paths)) + " java files\n")

# check for file is test or not
def control_file(file):
    fread = file.readlines()
    for line in fread:
        if '@test' in line:
            return True
    return False


# count per directory and running count
count = 0
paths = {}
for root, filepath, filename in full_file_paths:
    found = False
    try:
        found = control_file(open(filepath,mode='r'))
    except Exception as e:
        print("File couldn't opened.")
        print("File Path = ", filepath)
        print(e)
        print("\nTrying to ignore error and count the tests...")
        found = control_file(open(filepath, mode='r', errors="ignore"))
    if found:
        count = count + 1
        subdir = os.path.relpath(root, searchDir)
        if subdir in paths:
            paths[subdir] = paths[subdir] + 1
        else:
            paths[subdir] = 1

formattedOutput = []
widestResult = 0
for path in paths.keys():
    formattedText = path + ":" + str(paths[path])
    formattedOutput.append(formattedText)
    if len(formattedText) > widestResult:
        widestResult = len(formattedText)

rows, columns = os.popen('stty size', 'r').read().split()

widestResult = widestResult
if widestResult < 20:
    widestResult = 20
columnWidth = widestResult + 3
numcols = int(columns) / columnWidth
if numcols == 0:
    numcols = 1

col = 0
for text in sorted(formattedOutput):
    parts = text.split(':')
    padded = parts[0] + " " + ("." * (widestResult - len(text))) + " " + parts[1] + " "
    print(padded)
    col = col + 1
    if col >= numcols:
        col = 0
        print("")

print("\n\nFound " + str(count) + " java files containing @test\n")
