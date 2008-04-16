/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.testsuite;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipErrorEvent;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SecureServlet extends SipServlet implements SipErrorListener,
		Servlet, SipServletListener {

	private static Log logger = LogFactory.getLog(SecureServlet.class);
	
	
	/** Creates a new instance of SimpleProxyServlet */
	public SecureServlet() {
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		logger.info("the simple sip servlet has been started");
		super.init(servletConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doInvite(SipServletRequest request) throws ServletException,
			IOException {

		logger.info("Got request: "
				+ request.getMethod());
		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_RINGING);
		sipServletResponse.send();
		sipServletResponse = request.createResponse(SipServletResponse.SC_OK);
		sipServletResponse.send();
		
		try {
			SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
			SipApplicationSession appSession = 
        	sipFactory.createApplicationSession();
        	SipServletRequest req = sipFactory.createRequest(appSession,
        		"INVITE", "sip:from@127.0.0.1:5070", "sip:to@127.0.0.1:5058");
        	req.send();
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doBye(SipServletRequest request) throws ServletException,
			IOException {

		logger.info("Got BYE request: " + request);
		SipServletResponse sipServletResponse = request.createResponse(SipServletResponse.SC_OK);
		sipServletResponse.send();
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doResponse(SipServletResponse response)
			throws ServletException, IOException {
		
		SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
		if(response.getStatus() == 401) {
			// Avoid re-sending if the auth repeatedly fails.
			if(!"true".equals(getServletContext().getAttribute("FirstResponseRecieved")))
			{
				getServletContext().setAttribute("FirstResponseRecieved", "true");
				AuthInfo authInfo = sipFactory.createAuthInfo();
				authInfo.addAuthInfo(401, "sip-servlets-realm", "user", "pass");
				SipServletRequest req = sipFactory.createRequest(response.getApplicationSession(),
						"INVITE", "sip:from@127.0.0.1:5070", "sip:to@127.0.0.1:5070");
				req.addAuthHeader(response, authInfo);
				req.send();
			}
		}
		
		logger.info("Got response: " + response);
		super.doResponse(response);
	}

	// SipErrorListener methods

	/**
	 * {@inheritDoc}
	 */
	public void noAckReceived(SipErrorEvent ee) {
		logger.error("noAckReceived.");
	}

	/**
	 * {@inheritDoc}
	 */
	public void noPrackReceived(SipErrorEvent ee) {
		logger.error("noPrackReceived.");
	}

	public void servletInitialized(SipServletContextEvent ce) {
		try {
			SipFactory sipFactory = (SipFactory) ce.getServletContext().getAttribute(SIP_FACTORY);
			SipApplicationSession appSession = 
        	sipFactory.createApplicationSession();
        	SipServletRequest req = sipFactory.createRequest(appSession,
        		"INVITE", "sip:from@127.0.0.1:5070", "sip:to@127.0.0.1:5070");
        	req.send();
        } catch (Exception e) {
        	e.printStackTrace();
        }
		
	}

}