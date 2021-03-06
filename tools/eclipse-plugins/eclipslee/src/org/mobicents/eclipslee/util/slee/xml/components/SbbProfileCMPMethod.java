/**
 *   Copyright 2005 Open Cloud Ltd.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.mobicents.eclipslee.util.slee.xml.components;

import org.mobicents.eclipslee.util.slee.xml.DTDHandler;
import org.mobicents.eclipslee.util.slee.xml.DTDXML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * @author allenc
 */
public class SbbProfileCMPMethod extends DTDXML {

	protected SbbProfileCMPMethod(Document document, Element root, DTDHandler dtd) {
		super(document, root, dtd);
	}
	
	public void setDescription(String desc) {
		setChildText(getRoot(), "description", desc);
	}
	
	public String getDescription() {
		return getChildText(getRoot(), "description");
	}
	
	public void setProfileCMPMethodName(String name) {
		setChildText(getRoot(), "get-profile-cmp-method-name", name);
	}
	
	public String getProfileCMPMethodName() {
		return getChildText(getRoot(), "get-profile-cmp-method-name");
	}
	
	protected void setProfileSpecAliasRef(SbbProfileSpecRefXML ref) {
		setChildText(getRoot(), "profile-spec-alias-ref", ref.getAlias());
	}
	
	public String getProfileSpecAliasRef() {
		return getChildText(getRoot(), "profile-spec-alias-ref");
	}
}
