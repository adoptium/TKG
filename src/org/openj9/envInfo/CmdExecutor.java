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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

public class CmdExecutor {
	private static CmdExecutor instance;

	private CmdExecutor() {
	}

	public static CmdExecutor getInstance() {
		if (instance == null) {
			instance = new CmdExecutor();
		}
		return instance;
	}

	public String execute(String[] commands) {
		String rt = null;
		try {
			ProcessBuilder builder = new ProcessBuilder(Arrays.asList(commands));
			Map<String, String> env = builder.environment();
			// clear env and copy from the parent shell environment
			env.clear();
			Map<String, String> shellEnv = System.getenv();
			env.putAll(shellEnv);
			builder.redirectErrorStream(true);
			Process proc = builder.start();
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
			return "Command could not be executed";
		}
		return rt;
	}
}
