/*
 * Mobicents, Communications Middleware
 * 
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party
 * contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 *
 * Boston, MA  02110-1301  USA
 */
package org.mobicents.slee.resource.diameter.base.events;

import net.java.slee.resource.diameter.base.events.ExtensionDiameterMessage;

import org.jdiameter.api.Message;

/**
 * 
 * Start time:19:52:31 2009-05-28<br>
 * Project: diameter-parent<br>
 * 
 * 
 * Implementation of {@link ExtensionDiameterMessage}
 * 
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 * @see DiameterMessageImpl
 */
public class ExtensionDiameterMessageImpl extends DiameterMessageImpl implements ExtensionDiameterMessage {

	@Override
	public String getLongName() {
		// FIXME: baranowb; not documented
		return "Extension-Message";
	}

	@Override
	public String getShortName() {
		// FIXME: baranowb; not documented
		return "EM";
	}

	public ExtensionDiameterMessageImpl(Message message) {
		super(message);
	}

}
