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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.lang.management.ManagementFactory;

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

	// Console
	public static final String[] NUM_JAVA_PROCESSES_CMD = new String[] {"bash", "-c", "ps -ef | grep -i [j]ava | wc -l"};
	public static final String[] ACTIVE_JAVA_PROCESSES_CMD = new String[] {"bash", "-c", "ps -ef | grep -i [j]ava"};

	private Map<String, String> infoMap;
	
	public MachineInfo() {
		this.infoMap = new LinkedHashMap<>();
	}

	public void getInfo() {
		getSysInfo();
		getRuntimeInfo();
		getSpaceInfo("");
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

	private String execCommands(String[] commands) {
		String rt = null;
		try {
			Process proc = Runtime.getRuntime().exec(commands);
			BufferedReader stdOutput = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			String newline = "";  
			while ((line = stdOutput.readLine()) != null) {
				sb.append(newline).append(line);
				newline = "\n";
			}
			rt = sb.toString();
			proc.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return rt;
	}

	private void getSysInfo() {
		infoMap.put("uname", execCommands(UNAME_CMD));
		infoMap.put("cpuCores", execCommands(CPU_CORES_CMD));
		infoMap.put("sysArch", execCommands(SYS_ARCH_CMD));
		infoMap.put("procArch", execCommands(PROC_ARCH_CMD));
		infoMap.put("sysOS", execCommands(SYS_OS_CMD));
		infoMap.put("ulimit", execCommands(ULIMIT_CMD));
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
