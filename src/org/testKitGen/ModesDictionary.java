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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ModesDictionary {
	private Arguments arg;
	private String modesXml;
	private String ottawaCsv;
	private Map<String, String> spec2platMap;
	private Map<String, List<String>> invalidSpecsMap;
	private Map<String, String> clArgsMap;
	private String spec;

	public ModesDictionary(Arguments arg) {
		this.arg = arg;
		modesXml= arg.getProjectRootDir() + "/TKG/" + Constants.MODESXML;
		ottawaCsv = arg.getProjectRootDir() + "/TKG/" + Constants.OTTAWACSV;
		spec2platMap = new HashMap<String, String>();
		invalidSpecsMap = new HashMap<String, List<String>>();
		clArgsMap = new HashMap<String, String>();
	}

	public void parse() {
		try {
			Element modes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(modesXml)
					.getDocumentElement();
			parseMode(modes);
			parseInvalidSpec(modes);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Modes data parsed from " + Constants.MODESXML + " and " + Constants.OTTAWACSV + ".\n");
	}

	private void parseMode(Element modes) {
		NodeList modesNodes = modes.getElementsByTagName("mode");
		for (int i = 0; i < modesNodes.getLength(); i++) {
			Element mode = (Element) modesNodes.item(i);
			StringBuilder sb = new StringBuilder();
			NodeList clArgsNodes = mode.getElementsByTagName("clArg");
			for (int j = 0; j < clArgsNodes.getLength(); j++) {
				sb.append(clArgsNodes.item(j).getTextContent().trim()).append(" ");
			}
			clArgsMap.put(mode.getAttribute("number"), sb.toString().trim());
		}
	}

	private void setSpec() {
		String originalSpec = arg.getSpec();
		if (spec2platMap.containsKey(originalSpec)) {
			spec = originalSpec;
		} else {
			System.out.println("\nWarning: cannot find spec in " + Constants.OTTAWACSV + ".");
			if (originalSpec.contains("64")) {
				spec = "default-64";
				System.out.print("\tDetected 64 in spec. ");
			} else {
				spec = "default-none64";
				System.out.print("\tDidn't detect 64 in spec. ");
			}
			System.out.println("Use spec: " + spec + " to match mode.\n");
		}
	}

	private void parseInvalidSpec(Element modes) throws IOException {
		ArrayList<String> specs = new ArrayList<String>();
		int lineNum = 0;
		BufferedReader reader = null;
		if (arg.getSpec().toLowerCase().contains("zos")) {
			reader = Files.newBufferedReader(Paths.get(ottawaCsv), Charset.forName("IBM-1047"));
		} else {
			reader = Files.newBufferedReader(Paths.get(ottawaCsv));
		}
		String line = reader.readLine();
		while (line != null) {
			String[] fields = line.split(",");
			// Since the spec line has an empty title, we cannot do string match. We assume
			// the second line is spec.
			if (lineNum++ == 1) {
				specs.addAll(Arrays.asList(fields));
			} else if (fields[0].equals("plat")) {
				for (int i = 1; i < fields.length; i++) {
					spec2platMap.put(specs.get(i), fields[i]);
				}
			} else if (fields[0].startsWith("variation:")) {
				String modeNum = fields[0].substring("variation:".length());

				// Remove string Mode if it exists
				modeNum = modeNum.replace("Mode", "");

				NodeList modesNodes = modes.getElementsByTagName("mode");

				for (int i = 0; i < modesNodes.getLength(); i++) {
					Element mode = (Element) modesNodes.item(i);
					if (mode.getAttribute("number").equals(modeNum)) {
						ArrayList<String> invalidSpecs = new ArrayList<String>();
						for (int j = 1; j < fields.length; j++) {
							if (fields[j].equals("no")) {
								invalidSpecs.add(specs.get(j));
							}
						}
						// remove INGORESPECS from invalidSpecs array
						invalidSpecs = new ArrayList<String>(invalidSpecs.stream()
								.filter(c -> !Constants.INGORESPECS.contains(c)).collect(Collectors.toList()));
						// if invalidSpecs array is empty, set it to none
						if (invalidSpecs.size() == 0) {
							invalidSpecs.add("none");
						}
						invalidSpecsMap.put(modeNum, invalidSpecs);
						break;
					}
				}
			}
			line = reader.readLine();
		}
		setSpec();
	}

	public String getClArgs(String mode) {
		String rt = "";
		if (clArgsMap.containsKey(mode)) {
			rt = clArgsMap.get(mode);
		} else {
			System.out.println("\nWarning: cannot find mode " + mode + " to fetch jvm options");
		}
		return rt;
	}

	public boolean isValidMode(String mode) {
		boolean rt = true;
		// It is normal that certain mode cannot be found in ottawa.csv, in which case
		// it means no invalid specs
		if (invalidSpecsMap.containsKey(mode)) {
			rt = !invalidSpecsMap.get(mode).contains(spec);
		}
		return rt;
	}

	public String getPlat() {
		String rt = "";
		if (spec2platMap.containsKey(spec)) {
			rt = spec2platMap.get(spec);
		}
		return rt;
	}
}