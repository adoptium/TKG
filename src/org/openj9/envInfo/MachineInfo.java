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
import java.util.List;
import java.util.Map;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import com.sun.management.OperatingSystemMXBean;

public class MachineInfo {
	public static final String[] UNAME_CMD = new String[] {"uname", "-a"};
	public static final String[] SYS_ARCH_CMD = new String[] {"uname", "-m"};
	public static final String[] PROC_ARCH_CMD = new String[] {"uname", "-p"};
	public static final String[] ULIMIT_CMD = new String[] {"bash", "-c", "ulimit -a"};

	public static final String[] INSTALLED_MEM_CMD = new String[] {"bash", "-c", "grep MemTotal /proc/meminfo | awk '{print $2}"};
	public static final String[] FREE_MEM_CMD = new String[] {"bash", "-c", "grep MemFree /proc/meminfo | awk '{print $2}"};
	public static final String[] CPU_CORES_CMD = new String[] {"bash", "-c", "cat /proc/cpuinfo | grep processor | wc -l"};
	public static final String[] CPU_CORES_CMD_MAC = new String[] {"bash", "-c", "sysctl -n hw.ncpu"};
	public static final String[] CPU_CORES_CMD_SOLARIS = new String[] {"bash", "-c", "psrinfo | wc -l"};
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


	private List<Info> infoList;
	
	public MachineInfo() {
		this.infoList = new ArrayList<Info>();
	}

	public void getInfo() {
		getSysInfo();
		getPrerequisiteInfo();
		getRuntimeInfo();
		getPhysicalMemoryInfo();
		getSpaceInfo();
		validateInfo();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		String newline = "";
		for (Info info : infoList) {
			String output = info.output != null ? info.output : "";
			if (output.contains("\n")) {
				output = "\n" + output;
				output = output.replaceAll("(?m)\\n", "\n\t");
			}
			sb.append(newline).append(info.name).append(" : ").append(output);
			newline = "\n";
		}
		return sb.toString();
	}

	private String parseInfo(String output) {
		if (output == null) return ""; 
		Pattern pattern = Pattern.compile("[0-9]+[.][0-9]+([.][0-9]+)?"); 
		Matcher matcher = pattern.matcher(output);
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
		for (Info info : infoList) {
			if (info.req != null) {
				String version = parseInfo(info.output);
				valid &= validateVersion(info.name, version, info.req);
			}
		}
		if (!valid) {
			System.exit(1);
		}
	}

	private void getSysInfo() {
		CmdExecutor ce = CmdExecutor.getInstance();
		infoList.add(new Info("uname", UNAME_CMD, ce.execute(UNAME_CMD), null));
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			infoList.add(new Info("cpuCores", CPU_CORES_CMD, ce.execute(CPU_CORES_CMD_MAC), null));
		} else if (System.getProperty("os.name").toLowerCase().contains("sunos")) {
			infoList.add(new Info("cpuCores", CPU_CORES_CMD, ce.execute(CPU_CORES_CMD_SOLARIS), null));
		} else {
			infoList.add(new Info("cpuCores", CPU_CORES_CMD, ce.execute(CPU_CORES_CMD), null));
		}
		infoList.add(new Info("sysArch", SYS_ARCH_CMD, ce.execute(SYS_ARCH_CMD), null));
		infoList.add(new Info("procArch", PROC_ARCH_CMD, ce.execute(PROC_ARCH_CMD), null));
		infoList.add(new Info("sysOS", SYS_OS_CMD, ce.execute(SYS_OS_CMD), null));
		infoList.add(new Info("ulimit", ULIMIT_CMD, ce.execute(ULIMIT_CMD), null));
	}
	
	private void getPrerequisiteInfo() {
		CmdExecutor ce = CmdExecutor.getInstance();
		infoList.add(new Info("antVersion", ANT_VERSION_CMD, ce.execute(ANT_VERSION_CMD), "1.9.6"));
		/* required version is 4.1, but some exception applies, do not verify for now*/
		infoList.add(new Info("makeVersion", MAKE_VERSION_CMD, ce.execute(MAKE_VERSION_CMD), null));
		infoList.add(new Info("perlVersion", PERL_VERSION_CMD, ce.execute(PERL_VERSION_CMD), "5.10.1"));
		infoList.add(new Info("curlVersion", CURL_VERSION_CMD, ce.execute(CURL_VERSION_CMD), "7.20.0"));
	}

	private void getSpaceInfo() {
		File file = Paths.get("").toAbsolutePath().toFile();
		infoList.add(new Info("File path", new String[] {"Paths.get(\"\").toAbsolutePath().toFile().getAbsolutePath()"}, file.getAbsolutePath(), null));
		infoList.add(new Info("Total space (bytes)", new String[] {"Paths.get(\"\").toAbsolutePath().toFile().getTotalSpace()"}, String.valueOf(file.getTotalSpace()), null));
		infoList.add(new Info("Free space (bytes)", new String[] {"Paths.get(\"\").toAbsolutePath().toFile().getFreeSpace()"}, String.valueOf(file.getFreeSpace()), null));
		infoList.add(new Info("Usable space (bytes)", new String[] {"Paths.get(\"\").toAbsolutePath().toFile().getUsableSpace()"}, String.valueOf(file.getUsableSpace()), null));
	}

	private void getRuntimeInfo() {
		infoList.add(new Info("vmVendor", new String[] {"ManagementFactory.getRuntimeMXBean().getSpecVendor()"}, ManagementFactory.getRuntimeMXBean().getSpecVendor(), null));
		infoList.add(new Info("vmVersion", new String[] {"ManagementFactory.getRuntimeMXBean().getVmVersion()"}, ManagementFactory.getRuntimeMXBean().getVmVersion(), null));
	}
	
	private void getPhysicalMemoryInfo() {
		OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		infoList.add(new Info("Total Physical Memory Size", new String[] {"osBean.getTotalPhysicalMemorySize()"}, String.valueOf(osBean.getTotalPhysicalMemorySize()), null));
		infoList.add(new Info("Free Physical Memory Size", new String[] {"osBean.getFreePhysicalMemorySize()"}, String.valueOf(osBean.getFreePhysicalMemorySize()), null));
	}
}
