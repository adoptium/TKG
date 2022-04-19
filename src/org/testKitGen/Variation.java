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

package org.testKitGen;

import java.util.ArrayList;
import java.util.List;

public class Variation {
	enum PrintStatus {
		PRINT_CMD, PRINT_DISABLED, DO_NOT_PRINT;
	}
	private String variation;
	private String subTestName;
	private String mode;
	private String jvmOptions;
	private boolean isValid;
	private List<String> disabledReasons;
	private PrintStatus status;
	private String prefix;

	public Variation(String subTestName, String variation) {
		this.subTestName = subTestName;
		this.variation = variation;
		this.isValid = true;
		this.jvmOptions = "";
		this.disabledReasons = new ArrayList<String>();
		this.status = PrintStatus.DO_NOT_PRINT;
		this.prefix = "";
	}

	public String getVariation() {
		return this.variation;
	}

	public String getSubTestName() {
		return this.subTestName;
	}

	public String getMode() {
		return this.mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getJvmOptions() {
		return this.jvmOptions;
	}

	public void setJvmOptions(String jvmOptions) {
		this.jvmOptions = jvmOptions;
	}

	public boolean isValid() {
		return this.isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public List<String> getDisabledReasons() {
		return this.disabledReasons;
	}

	public void addDisabledReasons(String disabledReasons) {
		this.disabledReasons.add(disabledReasons);
	}

	public boolean isDisabled() {
		return getDisabledReasons().size() != 0;
	}

	public void setStatus(PrintStatus status) {
		this.status = status;
	}

	public PrintStatus getStatus() {
		return status;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getPrefix() {
		return prefix;
	}
}