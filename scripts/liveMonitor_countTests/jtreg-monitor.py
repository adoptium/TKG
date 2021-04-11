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

from sys import stdin, argv
import re
import datetime
import time

testestimate = ""
testestimatetitle = ""
esttestint = 0
if len(argv) == 2:
    testestimate = "/" + argv[1]
    esttestint = int(argv[1])
    testestimatetitle = "(estimated " + argv[1] + " tests) "

testResults = {}
counts = {}
start = datetime.datetime.now()
lastTest = ""
total = 0
clearStatus = ""
statusColors = { "Passed": u"\u001b[32m", "Error": u"\u001b[31m", "Failed": u"\u001b[31m" }
resetColor = u"\u001b[0m"
currentTest = ""
print(resetColor + "\n============== MONITORING STDIN FOR JTREG RESULTS " + testestimatetitle + "==============\n")
for line in stdin:
    # for debugging text file input 
    #time.sleep(1)
    line = line.strip()
    now = datetime.datetime.now()
    if line.startswith("TEST: "):
        currentTest = line[len("TEST: "):]
        testStarted = now
        total = total + 1
    elif line.startswith("TEST RESULT: "):
        m = re.search(r'TEST RESULT:\s*([A-Za-z]+)', line)
        status = m.group(1)
        line = line[len("TEST RESULT: "):]
        #line = now.strftime("%Y-%m-%d %H:%M:%S") + " " + "[Test: " + str(total) + "] " + line
        if status in counts:
            counts[status] = counts[status] + 1
        else:
            counts[status] = 1
            testResults[status] = []

        if status == "Passed":
            info = status 
            postfix = ""
            prefix = ""
        else:
            info = "\n-------> " + line
            if postfix == "":
                prefix = "\n"
            else:
                prefix = ""
            postfix = "\n"

        summaryText = "TEST (" + str(total) + ") {}".format(now - testStarted)+ " @ " + testStarted.strftime("%Y-%m-%d %H:%M:%S") + "): " + currentTest + " " + info 
        finalText = "TEST (" + str(total) + testestimate + ") {}".format(now - testStarted)+ " @ " + testStarted.strftime("%Y-%m-%d %H:%M:%S") + "): " + currentTest + " " + info 
        print(clearStatus + prefix + statusColors[status] + finalText + resetColor + postfix)
        clearStatus = ""

        testResults[status].append(summaryText + postfix)

    else:
        continue

    maxlen = 60
    if len(currentTest) > maxlen:
        running = "..." + currentTest[slice(len(currentTest)-maxlen,len(currentTest))]
    else:
        running = currentTest
    nextStatus = '{}'.format(now - start) + " / " + running + " / " + str(counts)
    if esttestint > 0:
        percentage = str(int(float(100) * (float(total) / float(esttestint))))
        nextStatus = percentage + "% / " + nextStatus
    print (clearStatus + nextStatus + "\r")
    clearStatus = (" " * len(nextStatus)) + "\r"

print(clearStatus)
print("\n\n============== RESULTS ==============\n")
for status in testResults:
    results = testResults[status]
    num = len(results)
    count = 1
    print("\n" + statusColors[status] + "============== " + status + " (" + str(num) + ") ==============\n" + resetColor)
    for result in results:
        print(statusColors[status] + status + " (" + str(count) + "/" + str(num) + ") - " + resetColor + result)
        count = count + 1

print("\n\n============== SUMMARY ==============\n")
print('Started: ' + start.strftime("%Y-%m-%d %H:%M:%S"))
print('Ended: ' + now.strftime("%Y-%m-%d %H:%M:%S"))
print('Time elapsed {}'.format(now - start))
print("Number of tests: " + str(total - 1))
print("Results: " + str(counts) + "\n")
