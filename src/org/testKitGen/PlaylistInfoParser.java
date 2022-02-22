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
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class PlaylistInfoParser {
	private Arguments arg;
	private ModesDictionary md;
	private TestTarget tt;
	private File playlistXML; 
	private File playlistXSD;
	private BuildList bl;

	public PlaylistInfoParser(Arguments arg, ModesDictionary md, TestTarget tt, File playlistXML) {
		this.arg = arg;
		this.md = md;
		this.tt = tt;
		this.playlistXML = playlistXML;
		this.playlistXSD = new File(Constants.PLAYLISTXSD);
	}

	private static class xsdErrorHandler extends DefaultHandler {
		@Override
		public void warning(SAXParseException e) throws SAXException {
			throw e;
		}
		
		@Override
		public void error(SAXParseException e) throws SAXException {
			throw e;
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXException {
			throw e;
		}
	 }
	
	private void validate() {
		if (playlistXML != null) {
			try {
				Validator validator = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(playlistXSD).newValidator();
				validator.setErrorHandler(new xsdErrorHandler());
				validator.validate(new StreamSource(playlistXML));
			} catch (SAXParseException e) {
				System.err.println("Error: schema validation failed for " + playlistXML.getAbsolutePath() + " with exception:\n");
				System.err.println("Line: "+e.getLineNumber());
				System.err.println("Column: "+e.getColumnNumber());
				System.err.println("Message: "+e.getMessage());
				System.exit(1);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	public PlaylistInfoParser(Arguments arg, ModesDictionary md, TestTarget tt, File playlistXML, BuildList bl) {
		this.arg = arg;
		this.md = md;
		this.tt = tt;
		this.playlistXML = playlistXML;
		this.bl = bl;
		this.playlistXSD = new File(Constants.PLAYLISTXSD);
	}

	public PlaylistInfo parse() {
		validate();
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
						pli.addInclude(currentElement.getTextContent());
					} else if (currentElement.getNodeName().equals("test")) {
						TestInfoParser parser = new TestInfoParser(arg, md, currentElement);
						TestInfo ti = parser.parse();
						if (ti != null) {
							boolean filterResult = tt.filterTestInfo(ti);
							if (filterResult) {
								testInfoList.add(ti);
								TestInfo.countTests(ti, tt);
								if (bl != null) {
									bl.add(playlistXML);
								}
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