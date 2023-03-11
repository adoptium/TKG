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

import xml.etree.ElementTree as ET
import argparse as ag

def parse_apache_2_license(license_file):
    with open(license_file, 'r') as f:
        license_lines = f.readlines()

        license_lines = license_lines[190:201]
        f.close()
    license_lines = ''.join(['#' + line for line in license_lines])
    return license_lines

def remove_duplicate_test_tags(xml_file, license_file):
    tree = ET.parse(xml_file, 
                    parser=ET.XMLParser(target=ET.TreeBuilder(insert_comments=True)))
    root = tree.getroot()
    test_case_names = []
    for test in root.findall('test'):
        test_case_name = test.find('testCaseName').text
        if test_case_name in test_case_names:
            root.remove(test)
        else:
            test_case_names.append(test_case_name)
    apache_license = '<!--\n' + parse_apache_2_license(license_file) + '-->\n'
    
    tree.write(xml_file, encoding='UTF-8', xml_declaration=True)
    
    # add license comment
    with open(xml_file, 'r+') as f:
        content = f.readlines()
        content.insert(1, apache_license)
        content = ''.join(content)
        f.seek(0, 0)
        f.write(content)
        f.close()

if __name__ == '__main__':
    parser = ag.ArgumentParser()
    parser.add_argument('-f','--xml-file', type = str, help='Path to playlist.xml', 
                        required=True)
    parser.add_argument('-l','--license-file', type = str, help='Path to Apache 2.0 LICENSE', 
                        required=True, default=None)
    args = parser.parse_args()
    remove_duplicate_test_tags(args.xml_file, args.license_file)
