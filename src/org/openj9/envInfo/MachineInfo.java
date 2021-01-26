/*******************************************************************************
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package org.openj9.envInfo;

import java.io.File;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class MachineInfo {
	public static final String[] UNAME_CMD = new String[] {"uname", "-a"};
	public static final String[] SYS_ARCH_CMD = new String[] {"uname", "-m"};
	public static final String[] PROC_ARCH_CMD = new String[] {"uname", "-p"};
	public static final String[] ULIMIT_CMD = new String[] {"bash", "-c", "ulimit -a"};

	public static final String[] INSTALLED_MEM_CMD = new String[] {"bash", "-c", "grep MemTotal /proc/meminfo | awk '{print $2}"};
	public static final String[] FREE_MEM_CMD = new String[] {"bash", "-c", "grep MemFree /proc/meminfo | awk '{print $2}"};
	public static final String[] CPU_CORES_CMD = new String[] {"bash", "-c", "cat /proc/cpuinfo | grep processor | wc -l"};

	public static final String[] NUMA_CMD = new String[] {"bash", "-c", "numactl --show | grep 'No NUMA support available on this system"};
	public static final String[] SYS_VIRT_CMD = new String[] {""};

	// Software
	public static final String[] SYS_OS_CMD = new String[] {"uname", "-s"};
	public static final String[] KERNEL_VERSION_CMD = new String[] {"uname", "-r"};
	public static final String[] GCC_VERSION_CMD = new String[] {"gcc", "-dumpversion"};

	public static final String[] XLC_VERSION_CMD = new String[] {"bash", "-c", "xlC -qversion | grep 'Version' "};
	public static final String[] GDB_VERSION_CMD = new String[] {"bash", "-c", "gdb --version | head -1"}; // debugger on Linux
	public static final String[] LLDB_VERSION_CMD = new String[] {"lldb", "--version"}; // debugger on Darwin/Mac
	public static final String[] GCLIBC_VERSION_CMD = new String[] {"bash", "-c", "ldd --version | head -1"};

	public static final String[] ANT_VERSION_CMD = new String[] {"bash", "-c", "ant -version"};
	public static final String[] MAKE_VERSION_CMD = new String[] {"bash", "-c", "make --version"};
	public static final String[] PERL_VERSION_CMD = new String[] {"bash", "-c", "perl --version"};
	public static final String[] CURL_VERSION_CMD = new String[] {"bash", "-c", "curl --version"};


	// Console
	public static final String[] NUM_JAVA_PROCESSES_CMD = new String[] {"bash", "-c", "ps -ef | grep -i [j]ava | wc -l"};
	public static final String[] ACTIVE_JAVA_PROCESSES_CMD = new String[] {"bash", "-c", "ps -ef | grep -i [j]ava"};

	public static Map<String, String> requirements;
	static {
		requirements = new HashMap<>();
		requirements.put("antVersion", "1.9.6");
   	 	requirements.put("makeVersion", "4.1");
 		requirements.put("perlVersion", "5.10.1");
		requirements.put("curlVersion", "7.20.0");
	}

	private Map<String, String> infoMap;
	
	public MachineInfo() {
		this.infoMap = new LinkedHashMap<>();
	}

	public void getInfo() {
		getSysInfo();
		getRuntimeInfo();
		getSpaceInfo("");
		validateInfo();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String newline = "";
		for (Map.Entry<String, String> entry : infoMap.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue() != null ? entry.getValue() : "";
			if (value.contains("\n")) {
				value = "\n" + value;
				value = value.replaceAll("(?m)\\n", "\n\t");
			}
			sb.append(newline).append(key).append(" : ").append(value);
			newline = "\n";
		}
		return sb.toString();
	}

	private String parseInfo(String version) {
		Pattern pattern = Pattern.compile("[0-9]+[.][0-9]+([.][0-9]+)?"); 
		Matcher matcher = pattern.matcher(version);
		if (matcher.find()) {
			return matcher.group(0);
		} else {
			return "";
		}
	}

	private ArrayList<Integer> versionStr2ArrList(String version) throws NumberFormatException {
		String[] versionSplit = version.split("\\.");
		ArrayList<Integer> versionArr = new ArrayList<Integer>();
		for (int i=0; i < versionSplit.length; i++) {
			versionArr.add(Integer.parseInt(versionSplit[i]));
		}
		return versionArr;
	}

	private void makeSameLength(ArrayList<Integer> list, int requiredLength){
		int differenceInLength = requiredLength - list.size();
		for (int i=0; i<differenceInLength; i++){
			list.add(0);
		}
	}

	private boolean validateVersion(String versionName, String actualVersionStr, String requriedVersionStr) {
		boolean isValid = true;
		try { 
			ArrayList<Integer> accVer = versionStr2ArrList(actualVersionStr);
			ArrayList<Integer> reqVer = versionStr2ArrList(requriedVersionStr);
			int accVerLen = accVer.size();
			int reqVerLen = reqVer.size();
			if (accVerLen > reqVerLen) {
				makeSameLength(reqVer, accVerLen);
			} else if (accVerLen < reqVerLen) {
				makeSameLength(accVer, reqVerLen);
			}
			for (int i=0; i < accVer.size(); i++) {
				if (reqVer.get(i) > accVer.get(i)){
					isValid = false;
				} else if (reqVer.get(i) < accVer.get(i)) {
					break;
				}
			}
			if (!isValid) {
				System.out.println("Error: required " + versionName + ": " + requriedVersionStr + ". Installed version: " + actualVersionStr);
			}
		} catch (NumberFormatException e){
			System.out.println("Warning: "+ versionName + " information cannot be extracted.");
			System.out.println(versionName + " output: " + actualVersionStr);
		}
		return isValid;
	}

	private void validateInfo() {
		boolean valid = true;
		for (Map.Entry<String, String> entry : requirements.entrySet()) {
			String version = parseInfo(infoMap.get(entry.getKey()));
			valid &= validateVersion(entry.getKey(), version, entry.getValue());
		}
		if (!valid) {
			/*System.out.println("\n");
			System.exit(1);*/
		}
	}

	private void getSysInfo() {
		CmdExecutor ce = CmdExecutor.getInstance();
		infoMap.put("uname", ce.execute(UNAME_CMD));
		infoMap.put("cpuCores", ce.execute(CPU_CORES_CMD));
		infoMap.put("sysArch", ce.execute(SYS_ARCH_CMD));
		infoMap.put("procArch", ce.execute(PROC_ARCH_CMD));
		infoMap.put("sysOS", ce.execute(SYS_OS_CMD));
		infoMap.put("ulimit", ce.execute(ULIMIT_CMD));
		infoMap.put("antVersion", ce.execute(ANT_VERSION_CMD));
		infoMap.put("makeVersion", ce.execute(MAKE_VERSION_CMD));
		infoMap.put("perlVersion", ce.execute(PERL_VERSION_CMD));
		infoMap.put("curlVersion", ce.execute(CURL_VERSION_CMD));
	}

	private void getSpaceInfo(String path) {
		Path userPath = Paths.get(path);
		File file = userPath.toAbsolutePath().toFile();
		infoMap.put("File path", file.getAbsolutePath());
		infoMap.put("Total space (bytes)", String.valueOf(file.getTotalSpace()));
		infoMap.put("Free space (bytes)", String.valueOf(file.getFreeSpace()));
		infoMap.put("Usable space (bytes)", String.valueOf(file.getUsableSpace()));
	}

	private void getRuntimeInfo() {
		infoMap.put("vmVendor", ManagementFactory.getRuntimeMXBean().getSpecVendor());
		infoMap.put("vmVersion", ManagementFactory.getRuntimeMXBean().getVmVersion());
		infoMap.put("Total memory (bytes)", String.valueOf(Runtime.getRuntime().totalMemory()));
		infoMap.put("Free memory (bytes)", String.valueOf(Runtime.getRuntime().freeMemory()));
	}
}
