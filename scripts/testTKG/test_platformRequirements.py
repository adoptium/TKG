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

import subprocess
from utils import *

def run_test():
	rt = True
	printTestheader("platformRequirements")

	buildList = "TKG/examples/platformRequirements"
	command = "make _all"
	print(f"\t{command}")
	result = subprocess.run(f"{EXPORT_BUILDLIST}={buildList}; {CD_TKG}; {MAKE_CLEAN}; {MAKE_COMPILE}; {command}", stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, check=False)

	stdout = result.stdout.decode()
	specStr = re.search(r"set SPEC to (.*)", stdout)

	if specStr is not None:
		spec = specStr.group(1)
	else:
		printError("Could not parse spec from output.")
		return False

	passed = set()
	skipped = set()

	if 'linux' in spec:
		skipped.add('test_not_linux_arch_x86_0')
	else:
		if 'x86' in spec:
			passed.add('test_not_linux_arch_x86_0')
		else:
			skipped.add('test_not_linux_arch_x86_0')

	if 'x86' in spec:
		passed.add('test_arch_x86_0')
		skipped.add('test_arch_nonx86_0')
	else:
		passed.add('test_arch_nonx86_0')
		skipped.add('test_arch_x86_0')

	if '64' in spec:
		passed.add('test_bits_64_0')
	else:
		skipped.add('test_bits_64_0')

	if 'osx' in spec:
		passed.add('test_os_osx_0')
	else:
		skipped.add('test_os_osx_0')

	if 'osx_x86-64' in spec:
		passed.add('test_osx_x86-64_0')
	else:
		skipped.add('test_osx_x86-64_0')

	if 'x86' in spec or '390' in spec:
		passed.add('test_arch_x86_390_0')
	else:
		skipped.add('test_arch_x86_390_0')

	if 'osx_x86-64' in spec or ('win_x86' in spec and 'win_x86-64' not in spec) or 'aix_ppc-64' in spec:
		passed.add('test_osx_x86-64_win_x86_aix_ppc-64_0')
	else:
		skipped.add('test_osx_x86-64_win_x86_aix_ppc-64_0')

	os_label = re.search(r"set OS_LABEL to (.*)", stdout)
	print("os_label in : {}".format(os_label))
	if os_label is not None:
		os_label = os_label.group(1)
		label_str = os_label.split(".")
		# os_label example: ubuntu.22
		try:
			ver = int(label_str[1],10)
			if label_str[0] == "ubuntu":
				if ver >= 22:
					passed.add('test_os_linux_ubuntu22_0')
				if ver >= 20:
					passed.add('test_os_linux_ubuntu20plus_0')
					passed.add('test_os_linux_ubuntu20plus_rhel8plus_0')
				if ver < 20:
					passed.add('test_not_os_linux_ubuntu20plus_0')

			if label_str[0] == "rhel":
				if ver >= 8:
					passed.add('test_os_linux_ubuntu20plus_rhel8plus_0')
		except ValueError as ve:
			print ("warning: os version value failed to convert to an integer")
			passed.add('test_not_os_linux_ubuntu20plus_0')
	else:
		passed.add('test_not_os_linux_ubuntu20plus_0')

	if 'test_os_linux_ubuntu20plus_0' not in passed:
		skipped.add('test_os_linux_ubuntu20plus_0')

	if 'test_os_linux_ubuntu22_0' not in passed:
		skipped.add('test_os_linux_ubuntu22_0')

	if 'test_os_linux_ubuntu20plus_rhel8plus_0' not in passed:
		skipped.add('test_os_linux_ubuntu20plus_rhel8plus_0')

	if 'test_not_os_linux_ubuntu20plus_0' not in passed:
		skipped.add('test_not_os_linux_ubuntu20plus_0')

	micro_arch = re.search(r"set MICROARCH to (.*)", stdout)
	# micro_arch = "set MICROARCH to z14"
	print("micro_arch in platform req: {}".format(micro_arch))
	pattern = r"set MICROARCH to (.*)"
	# micro_arch = re.search(pattern, micro_arch)
	if micro_arch is not None:
		print(micro_arch)
		micro_arch = micro_arch.group(1)
		label_str = micro_arch
		print("Label_str:",label_str)
		# micro_arch example: skylake
		try:
			ver = int(''.join(filter(str.isdigit, micro_arch.split()[-1])))
			print("ver:",ver)
			if label_str == "skylake":
				passed.add('test_arch_x86_skylake_0')
			if ver == 13:
				passed.add('test_arch_390_z13_0')
			elif ver > 13:
				passed.add('test_arch_390_z13plus_0')
			if ver == 14:
				passed.add('test_arch_390_z14_0')
			elif ver > 14:
				passed.add('test_arch_390_z14plus_0')
			if ver == 15:
				passed.add('test_arch_390_z15_0')
			elif ver > 15:
				passed.add('test_arch_390_z15plus_0')
			else:
				skipped.add('test_not_arch_390_0')
		except ValueError as ve:
			print("warning: microarch version value failed to convert to an integer")
			skipped.add('test_not_arch_390_0')
	else:
		passed.add('test_not_arch_390_0')
		passed.add('test_not_arch_390_z15plus_0')

	if 'test_arch_390_z15_0' not in passed:
		skipped.add('test_arch_390_z15_0')

	if 'test_arch_390_z14_0' not in passed:
		skipped.add('test_arch_390_z14_0')

	if 'test_arch_390_z13_0' not in passed:
		skipped.add('test_arch_390_z13_0')

	if 'test_arch_390_z15plus_0' not in passed:
		skipped.add('test_arch_390_z15plus_0')

	if 'test_arch_390_z14plus_0' not in passed:
		skipped.add('test_arch_390_z14plus_0')

	if 'test_arch_390_z13plus_0' not in passed:
		skipped.add('test_arch_390_z13plus_0')

	if 'test_arch_x86_skylake_0' not in passed:
		skipped.add('test_arch_x86_skylake_0')


	rt &= checkResult(result, passed, set(), set(), skipped)
	return rt

if __name__ == "__main__":
	run_test()