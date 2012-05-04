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
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.kivadoc.DocRepository;
import org.kivadoc.FullDocumentParser;
import org.kivadoc.TargetExtractor;
import org.kivadoc.KivaDocument;
import org.kivadoc.Uri;
import org.kivadoc.utils.UtfUtils;

public class DirectoryDocRepository implements DocRepository {
	private File inputPath;
	private HashMap<File, KivaDocument> parseMap = new HashMap<File, KivaDocument>();
	
	public DirectoryDocRepository(File inputPath) {
		this.inputPath = inputPath;
	}

	public KivaDocument getParsed(File f) throws IOException {
		if (!parseMap.containsKey(f)) {
			FullDocumentParser parser = new FullDocumentParser();
			byte[] fileBytes = FileUtils.readFileToByteArray(f);
			parseMap.put(f, parser.parse(UtfUtils.decode(fileBytes)));
		}
		return parseMap.get(f);
	}
	
	@Override
	public String getTitle(String uri) {
		try {
			return getParsed(getDocFile(uri)).getTitle();
		} catch (IOException e) {
		}
		return Uri.baseName(uri);
	}

	@Override
	public boolean doesResourceExist(String uri) {
		return new File(inputPath, uri).exists();
	}

	@Override
	public Set<String> getAllTargets(String uri) {
		try {
			return TargetExtractor.extract(getParsed(getDocFile(uri)).getBody());
		} catch (IOException e) {
			return null;
		}
	}

	private File getDocFile(String uri) {
		return new File(inputPath, uri + ".txt");
	}

}
