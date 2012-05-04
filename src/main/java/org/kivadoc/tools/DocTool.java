/**
 * Copyright 2012 Hannu Kankaanpää
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 *
 * @author Hannu Kankaanpää <hannu.kp@gmail.com>
 */
package org.kivadoc.tools;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.kivadoc.DocRepository;
import org.kivadoc.OrphanDetector;
import org.kivadoc.KivaDocument;
import org.kivadoc.KivaError;
import org.kivadoc.OrphanDetector.LinkGraph;
import org.kivadoc.OrphanDetector.Orphans;
import org.kivadoc.docparser.DocumentParser;
import org.kivadoc.emitters.FullHtmlEmitter;

public class DocTool {
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage: app [inputPath] [outputPath]");
			System.err.println();
			System.err.println("DocTool " + getVersion());
			System.exit(1);
		}
		
		String inputPath = args[0];
		String outputPath = args[1];
		
		DocTool d = new DocTool(new File(inputPath), new File(outputPath));
		d.run();
		if (d.errors) {
			System.exit(1);
		}
	}

	private static String getVersion() throws IOException {
		return IOUtils.toString(DocTool.class.getResourceAsStream("/org/kivadoc/tools/version.txt"));
	}
	
	private boolean errors = false;
	private File inputPath;
	private File outputPath;
	private DirectoryDocRepository docRepository;
	private Properties baseProperties = new Properties();
	private LinkGraph linkGraph = new LinkGraph();

	public DocTool(File inputPath, File outputPath) {
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.docRepository = new DirectoryDocRepository(inputPath);
	}

	private void run() throws IOException {
		loadBaseProperties();
		processPath(inputPath, outputPath, "/");
		printOrphanWarnings();
	}

	private void printOrphanWarnings() {
		String ignoreOrphansPattern = "^/" + DocRepository.BASE_PROPERTIES + "$";
		String ignoreOrphansString = baseProperties.getProperty("ignoreOrphans", "").trim();
		if (!ignoreOrphansString.isEmpty()) {
			ignoreOrphansPattern += "|" + ignoreOrphansString;
		}
		Orphans orphans = OrphanDetector.filterOutOrphans(
				OrphanDetector.findOrphans(linkGraph, "/home"),
				Pattern.compile(ignoreOrphansPattern));
		
		if (!orphans.getDocuments().isEmpty()) {
			System.err.println("Orphan documents:");
			for (String doc : orphans.getDocuments()) {
				System.err.println(doc);
			}
			System.err.println();
		}
		if (!orphans.getFiles().isEmpty()) {
			System.err.println("Orphan files:");
			for (String fn : orphans.getFiles()) {
				System.err.println(fn);
			}
			System.err.println();
		}
	}

	private void loadBaseProperties() throws IOException {
		File propertiesFile = new File(inputPath, DocRepository.BASE_PROPERTIES);
		if (propertiesFile.exists()) {
			FileInputStream fis = new FileInputStream(propertiesFile);
			try {
				baseProperties.load(fis);
			} finally {
				fis.close();
			}
		}
	}

	private void processPath(File path, File outputPath, String uri) throws IOException {
		for (String frel : path.list()) {
			File f = new File(path, frel);
			if (f.isDirectory() && !frel.startsWith(".")) {
				processPath(f, new File(outputPath, frel), uri + frel + "/");
			} else if (f.isFile()) {
				try {
					FileUtils.copyFileToDirectory(f, outputPath);
					if (frel.endsWith(".txt")) {
						KivaDocument doc = docRepository.getParsed(f);
						linkGraph.addDocument(uri + frel.substring(0, frel.length() - 4), doc.getBody());
						
						FullHtmlEmitter emitter = new FullHtmlEmitter();
						DirectoryDocHtmlEmitter docHtmlEmitter = new DirectoryDocHtmlEmitter(docRepository);
						docHtmlEmitter.setDocumentDirUri(uri);
						emitter.setDocHtmlEmitter(docHtmlEmitter);
						String htmlString = emitter.toHtmlString(doc);
						
						List<KivaError> errors = new ArrayList<KivaError>();
						errors.addAll(doc.getErrors());
						errors.addAll(emitter.getErrors());
						
						if (!errors.isEmpty()) {
							System.err.println("Errors in file " + StringEscapeUtils.escapeJava(uri + frel) + ":");
							for (KivaError error : errors) {
								System.err.println("-Line=" + error.getLineNumber() + ", " + error.getType());
							}
							System.err.println();
						}
						
						String extraStyleHtmlString = emitter.toExtraStyleHtmlString(doc);
						String result = IOUtils.toString(DocumentParser.class.getResourceAsStream("/org/kivadoc/template.html"));
						result = result.replace("**EXTRA_STYLES**", extraStyleHtmlString);
						result = result.replace("**TITLE**", StringEscapeUtils.escapeHtml(doc.getTitle()) + " -- kivadoc");
						result = result.replace("**BODY**", htmlString);
						
						File outHtmlFile = new File(outputPath, frel.substring(0, frel.length() - 4) + ".html");
						FileUtils.writeStringToFile(outHtmlFile, result, "UTF-8");
					} else {
						linkGraph.addFile(uri + frel);
					}
				} catch (Exception e) {
					System.err.println("Error when processing file " + StringEscapeUtils.escapeJava(f.toString()));
					e.printStackTrace();
					errors = true;
				}
			}
		}
	}
}
