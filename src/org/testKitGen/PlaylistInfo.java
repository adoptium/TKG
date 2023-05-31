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

public class PlaylistInfo {
	private List<TestInfo> testInfoList;
	private List<String> includeList;
	private List<String> ignoreOnRerunList;

	public PlaylistInfo() {
		this.includeList = new ArrayList<String>();
		this.testInfoList = new ArrayList<TestInfo>();
		this.ignoreOnRerunList = new ArrayList<String>();
	}

	public List<String> getIncludeList() {
		return includeList;
	}

	public void addInclude(String include) {
		this.includeList.add(include);
	}

	public List<TestInfo> getTestInfoList() {
		return testInfoList;
	}

	public void setTestInfoList(List<TestInfo> testInfoList) {
		this.testInfoList = testInfoList;
	}

	public void addIgnoreOnRerun(String test) {
		ignoreOnRerunList.add(test);
	}

	public List<String> getIgnoreOnRerunList() {
		return this.ignoreOnRerunList;
	}

	public boolean containsTest() {
		return getTestInfoList().size() > 0;
	}
}