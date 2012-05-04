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


import java.util.Set;

import org.kivadoc.DocRepository;
import org.kivadoc.emitters.DocHtmlEmitter;

public class DirectoryDocHtmlEmitter extends DocHtmlEmitter {
	private DocRepository docRepo;

	public DirectoryDocHtmlEmitter(DocRepository docRepo) {
		this.docRepo = docRepo;
	}

	@Override
	public String getTitle(DocTarget target) {
		String title = "";
		if (!target.isInternal()) {
			title = docRepo.getTitle(target.uri);
		}
		return makeTitleWithFragment(title, target.frag);
	}
	
	@Override
	protected boolean doesTargetExist(DocTarget target) {
		Set<String> targets = docRepo.getAllTargets(target.uri);
		return targets != null && (target.frag == null || targets.contains(target.frag));
	}
	
	@Override
	protected boolean doesResourceExist(String uri) {
		return docRepo.doesResourceExist(uri);
	}
}
