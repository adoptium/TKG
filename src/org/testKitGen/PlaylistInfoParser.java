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

public class PlaylistInfoParser {
	private Arguments arg;
	private ModesDictionary md;
	private TestTarget tt;
	private File playlistXML;

	public PlaylistInfoParser(Arguments arg, ModesDictionary md, TestTarget tt, File playlistXML) {
		this.arg = arg;
		this.md = md;
		this.tt = tt;
		this.playlistXML = playlistXML;
	}

	public PlaylistInfo parse() {
		PlaylistInfo pli = new PlaylistInfo();
		if (playlistXML == null) return pli;
		try {
			System.out.println("Parsing " + playlistXML.getAbsolutePath() + "\n");
			Document xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(playlistXML);
			NodeList childNodes = xml.getDocumentElement().getChildNodes();
			List<TestInfo> testInfoList = new ArrayList<TestInfo>();
			for (int i = 0; i < childNodes.getLength(); i++) {
				Node currentNode = childNodes.item(i);
				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
					Element currentElement = ((Element) currentNode);
					if (currentElement.getNodeName().equals("include")) {
						pli.setInclude(currentElement.getTextContent());
					} else if (currentElement.getNodeName().equals("test")) {
						TestInfoParser parser = new TestInfoParser(arg, md, currentElement);
						TestInfo ti = parser.parse();
						if (ti != null) {
							boolean filterResult = tt.filterTestInfo(ti);
							if (filterResult) {
								testInfoList.add(ti);
								TestInfo.countTests(ti, tt);
							}
						}
					}
				}
			}
			pli.setTestInfoList(testInfoList);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return pli;
	}
}