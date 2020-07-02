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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class PlaylistInfo {
	private Arguments arg;
	private ModesDictionary md;
	private File playlistXML;
	private List<TestInfo> testInfoList;
	private String include = null;

	public PlaylistInfo(Arguments arg, ModesDictionary md, File playlistXML) {
		this.arg = arg;
		this.md = md;
		this.playlistXML = playlistXML;
		this.testInfoList = new ArrayList<TestInfo>();
	}

	public String getInclude() {
		return include;
	}

	public List<TestInfo> getTestInfoList() {
		return testInfoList;
	}

	public boolean getParseResult() {
		return testInfoList.size() > 0;
	}

	public void parseInfo() {
		if (playlistXML == null) return;
		try {
			Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(playlistXML);
			NodeList childNodes = xml.getDocumentElement().getChildNodes();

			for (int i = 0; i < childNodes.getLength(); i++) {
				Node currentNode = childNodes.item(i);

				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
					Element currentElement = ((Element) currentNode);
					if (currentElement.getNodeName().equals("include")) {
						include = currentElement.getTextContent();
					} else if (currentElement.getNodeName().equals("test")) {
						TestInfo testInfo = new TestInfo(arg, md, currentElement);
						if (testInfo.parseInfo()) {
							testInfoList.add(testInfo);
						}
					}
				}
			}
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}