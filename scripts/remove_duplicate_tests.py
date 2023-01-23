import xml.etree.ElementTree as ET

def remove_duplicate_test_tags(xml_file):
    tree = ET.parse(xml_file)
    root = tree.getroot()
    test_case_names = []
    for test in root.findall('test'):
        test_case_name = test.find('testCaseName').text
        if test_case_name in test_case_names:
            root.remove(test)
        else:
            test_case_names.append(test_case_name)
    head_strings = '''<?xml version='1.0' encoding='UTF-8'?>
<!--
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
-->
'''
    #add head strings to the xml file
    tree.write(xml_file)
    with open(xml_file, 'r+') as f:
        content = f.read()
        f.seek(0, 0)
        f.write(head_strings)
        f.write(content)

if __name__ == '__main__':
    remove_duplicate_test_tags('playlist.xml')
