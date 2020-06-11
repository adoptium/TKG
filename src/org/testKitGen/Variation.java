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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Variation {
	private ModesDictionary md;
	private String variation;
	private String spec;
	private String platformRequirements;
	private String subTestName;
	private String mode;
	private String jvmOptions;
	private boolean isValid;

	public Variation(Arguments arg, ModesDictionary md, String subTestName, String variation, String platformRequirements) {
		this.md = md;
		this.subTestName = subTestName;
		this.spec = arg.getSpec();
		this.variation = variation;
		this.platformRequirements = platformRequirements;
		this.isValid = true;
		this.jvmOptions = "";
		parseVariation();
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

	public String getJvmOptions() {
		return this.jvmOptions;
	}

	public boolean isValid() {
		return this.isValid;
	}

	private void parseVariation() {
		jvmOptions = " " + variation + " ";

		jvmOptions = jvmOptions.replaceAll(" NoOptions ", "");

		Pattern pattern = Pattern.compile(".* Mode([^ ]*?) .*");
		Matcher matcher = pattern.matcher(jvmOptions);

		if (matcher.matches()) {
			mode = matcher.group(1).trim();
			String clArgs = md.getClArgs(mode);
			List<String> invalidSpecs = md.getInvalidSpecs(mode);
			if (invalidSpecs.contains(spec)) {
				isValid = false;
			}
			jvmOptions = jvmOptions.replace("Mode" + mode, clArgs);
		}

		jvmOptions = jvmOptions.trim();

		isValid &= checkPlatformReq();
	}

	private boolean checkPlatformReq() {
		if ((platformRequirements != null) && (!platformRequirements.trim().isEmpty())) {
			for (String pr : platformRequirements.split("\\s*,\\s*")) {
				pr = pr.trim();
				String[] prSplitOnDot = pr.split("\\.");
				String fullSpec = spec;

				// Special case 32/31-bit specs which do not have 32 or 31 in the name (i.e.
				// aix_ppc)
				if (!spec.contains("-64")) {
					if (spec.contains("390")) {
						fullSpec = spec + "-31";
					} else {
						fullSpec = spec + "-32";
					}
				}

				if (prSplitOnDot[0].charAt(0) == '^') {
					if (fullSpec.contains(prSplitOnDot[1])) {
						return false;
					}
				} else {
					if (!fullSpec.contains(prSplitOnDot[1])) {
						return false;
					}
				}
			}
		}
		return true;
	}

}