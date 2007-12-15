 /*
  * Mobicents: The Open Source SLEE Platform      
  *
  * Copyright 2003-2005, CocoonHive, LLC., 
  * and individual contributors as indicated
  * by the @authors tag. See the copyright.txt 
  * in the distribution for a full listing of   
  * individual contributors.
  *
  * This is free software; you can redistribute it
  * and/or modify it under the terms of the 
  * GNU Lesser General Public License as
  * published by the Free Software Foundation; 
  * either version 2.1 of
  * the License, or (at your option) any later version.
  *
  * This software is distributed in the hope that 
  * it will be useful, but WITHOUT ANY WARRANTY; 
  * without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR 
  * PURPOSE. See the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the 
  * GNU Lesser General Public
  * License along with this software; 
  * if not, write to the Free
  * Software Foundation, Inc., 51 Franklin St, 
  * Fifth Floor, Boston, MA
  * 02110-1301 USA, or see the FSF site:
  * http://www.fsf.org.
  */

package se.jayway.sip.slee.sbb;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;
import javax.slee.ActivityContextInterface;
import javax.slee.FactoryException;
import javax.slee.InitialEventSelector;
import javax.slee.RolledBackContext;
import javax.slee.SLEEException;
import javax.slee.SbbContext;
import javax.slee.SbbLocalObject;
import javax.slee.TransactionRequiredLocalException;
import javax.slee.UnrecognizedActivityException;

import org.apache.log4j.Logger;
import org.mobicents.slee.resource.sip.SipActivityContextInterfaceFactory;
import org.mobicents.slee.resource.sip.SipFactoryProvider;

import se.jayway.sip.util.CacheException;
import se.jayway.sip.util.CacheFactory;
import se.jayway.sip.util.CacheUtility;
import se.jayway.sip.util.Session;
import se.jayway.sip.util.SessionAssociation;
import se.jayway.sip.util.SipUtils;
import se.jayway.sip.util.SipUtilsFactorySingleton;
import se.jayway.sip.util.StateCallback;

public abstract class CallControlSbb implements javax.slee.Sbb {
	private static Logger log = Logger.getLogger(CallControlSbb.class);

	private SbbContext sbbContext; // This SBB's SbbContext

	private SipProvider sipProvider;
	
	private SipActivityContextInterfaceFactory activityContextInterfaceFactory;

	private SipUtils sipUtils;
	
	public abstract CallControlSbbActivityContextInterface asSbbActivityContextInterface(
			ActivityContextInterface aci);
	
	private CacheUtility cache;
		
	private String callControlSipAddress;
	private String password;
	
	public abstract ThirdPCCSbbUsage getDefaultSbbUsageParameterSet();
	/**
	 * Generate a custom convergence name so that events with the same call
	 * identifier will go to the same root SBB entity. For other methods, use
	 * ActivityContext.
	 */
	public InitialEventSelector callIdSelect(InitialEventSelector ies) {
		Object event = ies.getEvent();
		String callId = null;
		if (event instanceof ResponseEvent) {
			// If response event, the convergence name to callId
			Response response = ((ResponseEvent) event).getResponse();
			callId = ((CallIdHeader) response.getHeader(CallIdHeader.NAME))
					.getCallId();
		} else if (event instanceof RequestEvent) {
			// If request event, the convergence name to callId
			Request request = ((RequestEvent) event).getRequest();
			callId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME))
					.getCallId();
		} else {
			// If something else, use activity context.
			ies.setActivityContextSelected(true);
			return ies;
		}
		// Set the convergence name
		if(log.isDebugEnabled()) {
			log.debug("Setting convergence name to: " + callId);
		}
		ies.setCustomName(callId);
		return ies;
	}
	/**
	 * @see CallControlSbbLocalObject#terminate(String)
	 * @param guid
	 */
	public void terminate(String guid) {
		SessionAssociation association = (SessionAssociation) cache.get(guid);
		if( log.isDebugEnabled() ) {
			log.debug("Entering terminate with guid " + guid + " representing session association " + association);
		}
		
		Session session = association.getCalleeSession();
		try {
			sendRequest(session.getDialog(), Request.BYE);
			setState(new ExternalTerminationCalleeState(), session.getCallId());
		} catch (SipException e) {
			log.error("Exception sending BYE to callee in terminate, the session could not be teared down");
			setState(new TerminationState(), session.getCallId());
		}
	}
	
	/**
	 * @see CallControlSbbLocalObject#cancel(String)
	 * @param guid
	 */
	public void cancel(String guid) {
		SessionAssociation association = (SessionAssociation) cache.get(guid);
		if( log.isDebugEnabled() ) {
			log.debug("Entering canel with guid " + guid + " representing session association " + association);
		}
		
		String state = association.getState();
		Session calleeSession = association.getCalleeSession();
				
		if(InitialState.class.getName().equals(state) || 
		   CalleeTryingState.class.getName().equals(state) ||
		   CalleeRingingState.class.getName().equals(state)) {
           sendRequestCancel(calleeSession.getDialog());
			setState(new TerminationState(), calleeSession.getCallId());
			
		} else if (CallerInvitedState.class.getName().equals(state) || 
				   CallerTryingState.class.getName().equals(state) || 
				   CallerRingingState.class.getName().equals(state)) {
			//If callee has accepted, send CANCEL to caller and BYE to callee
			Session callerSession = association.getCallerSession();
			try {
				//Send BYE to callee
				sendRequest(calleeSession.getDialog(), Request.BYE);
				//Send CANCEL to caller
				sendRequestCancel(callerSession.getDialog());
				setState(new ExternalCancellationState(), calleeSession.getCallId());
			} catch (SipException e) {
				log.error("Exception sending BYE to callee in terminate, the session could not be teared down");
				setState(new TerminationState(), calleeSession.getCallId());
			}
		} else {
			// TODO Throw IllegalStateExeception, but how is it handled by the SLEE?
			if(log.isDebugEnabled()) {
				log.debug("External cancellation failed! Illegal state: "+state);
			}
		}
		
		
	}
	
	private void executeRequestState(RequestEvent event) {
		String callId = ((CallIdHeader)event.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
		SessionAssociation sa = (SessionAssociation) cache.get(callId);
		SimpleCallFlowState simpleCallFlowState = getState(sa.getState());
		simpleCallFlowState.execute(event);
	}

	private void executeResponseState(ResponseEvent event) {
		String callId = ((CallIdHeader) event.getResponse().getHeader(
				CallIdHeader.NAME)).getCallId();
		SessionAssociation sa = (SessionAssociation) cache.get(callId);
		SimpleCallFlowState simpleCallFlowState = getState(sa.getState());
		simpleCallFlowState.execute(event);
	}

	public void onAckEvent(RequestEvent event, ActivityContextInterface aci) {
		if(log.isDebugEnabled()) {
			log.debug("Received: ACK");
		}
		executeRequestState(event);
	}

	public void onByeEvent(RequestEvent event, ActivityContextInterface aci) {
		if(log.isDebugEnabled()) {
			log.debug("Received BYE");
		}
		executeRequestState(event);
	}

	public void onCancelEvent(RequestEvent event, ActivityContextInterface aci) {
		if(log.isDebugEnabled()) {
			log.debug("Received CANCEL");
		}
		executeRequestState(event);
	}

	

	public void onServerErrorRespEvent(ResponseEvent event,
			ActivityContextInterface aci) {
		if(log.isDebugEnabled()) {
			log.debug("Received server error response : " + event.getResponse().getStatusCode());
		}
		executeResponseState(event);
	}

	public void onClientErrorRespEvent(ResponseEvent event,
			ActivityContextInterface aci) {
		if(log.isDebugEnabled()) {
			log.debug("Received client error event : " + event.getResponse().getStatusCode());
		}
		executeResponseState(event);
	}

	public void onGlobalFailureRespEvent(ResponseEvent event,
			ActivityContextInterface aci) {
		if(log.isDebugEnabled()) {
			log.debug("Received global failure event : " + event.getResponse().getStatusCode());
		}
		executeResponseState(event);
	}

	public void onSuccessRespEvent(ResponseEvent event,
			ActivityContextInterface aci) {
		if(log.isDebugEnabled()) {
			log.debug("Received success response event " + event.getResponse().getStatusCode());
		}
		executeResponseState(event);
	}

	public void onInfoRespEvent(ResponseEvent event,
			ActivityContextInterface aci) {
		if(log.isDebugEnabled()) {
			log.debug("Received informational response event : " + event.getResponse().getStatusCode());
		}
		executeResponseState(event);
	}


	
	/*
	 * Timeouts
	 */
	public void onTransactionTimeoutEvent(TimeoutEvent event,
			ActivityContextInterface ac) {
		if(log.isDebugEnabled()) {
			log.debug("Received timeout event");
		}
	}

	public void onRetransmitTimeoutEvent(TimeoutEvent event,
			ActivityContextInterface ac) {
		if(log.isDebugEnabled()) {
			log.debug("Received retransmit timeout event");
		}
	}

	// TODO: Perform further operations if required in these methods.
	public void setSbbContext(SbbContext context) {
		this.sbbContext = context;
		
		try {
			// Create the cache used for the session association
			cache = CacheFactory.getInstance().getCache();

			Context myEnv = (Context) new InitialContext()
					.lookup("java:comp/env");
			// Getting JAIN SIP Resource Adaptor interfaces
			SipFactoryProvider factoryProvider = (SipFactoryProvider) myEnv
					.lookup("slee/resources/jainsip/1.1/provider");

			sipProvider = factoryProvider.getSipProvider();
			
			activityContextInterfaceFactory = (SipActivityContextInterfaceFactory) myEnv
					.lookup("slee/resources/jainsip/1.1/acifactory");
			
			// Check that callControlSipAddress and password are present
			callControlSipAddress = (String)myEnv.lookup("callControlSipAddress");
			password = (String)myEnv.lookup("password");
			String passwordDisplay = password == null ? "null": "*******";
			if(log.isDebugEnabled()) {
				log.debug("Checking that CallControlSbb callControlSipAddress and password are present : callControlSipAddress = " +
						callControlSipAddress + " password = " + passwordDisplay);
			}

			
			sipUtils = SipUtilsFactorySingleton.getInstance().getSipUtils();
			
		} catch (NamingException ne) {
			ne.printStackTrace();
		} catch (CacheException e) {
			e.printStackTrace();
		}
	}

	public void unsetSbbContext() {
		this.sbbContext = null;
	}

	// TODO: Implement the lifecycle methods if required
	public void sbbCreate() throws javax.slee.CreateException {
	}

	public void sbbPostCreate() throws javax.slee.CreateException {
	}

	public void sbbActivate() {
	}

	public void sbbPassivate() {
	}

	public void sbbRemove() {
	}

	public void sbbLoad() {
	}

	public void sbbStore() {
	}

	public void sbbExceptionThrown(Exception exception, Object event,
			ActivityContextInterface activity) {
	}

	public void sbbRolledBack(RolledBackContext context) {
	}

	/**
	 * Convenience method to retrieve the SbbContext object stored in
	 * setSbbContext. TODO: If your SBB doesn't require the SbbContext object
	 * you may remove this method, the sbbContext variable and the variable
	 * assignment in setSbbContext().
	 * 
	 * @return this SBB's SbbContext object
	 */

	protected SbbContext getSbbContext() {
		return sbbContext;
	}
	
	/**
	 * Store the client transaction in the cache
	 *	since we may need to send a cancel request associated with this
	 * ClientTransaction later.
	 * @param The client transaction to store as "to be cancelled".
	 */
	private void setToBeCancelledClientTransaction(ClientTransaction ct) {
		String callId = ((CallIdHeader) ct.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
		SessionAssociation sa = (SessionAssociation) cache.get(callId);
		Session session = sa.getSession(callId);
		session.setToBeCancelledClientTransaction(ct);
	}

	
	private SimpleCallFlowState getState(String classNameForState) {
		SimpleCallFlowState simpleCallFlowState = null;
		try {
			Class innerCls = Class.forName(classNameForState);
			Constructor c = innerCls.getDeclaredConstructors()[0];
			simpleCallFlowState = (SimpleCallFlowState) c
					.newInstance(new Object[] { this });
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return simpleCallFlowState;
	}

	public abstract class StateSupport implements SimpleCallFlowState {
		private String statusMessage;

		public StateSupport(String statusMessage) {
			this.statusMessage = statusMessage;
		}

		public String getStatusMessage() {
			return statusMessage;
		}
	}
	
	public abstract class BeforeCalleeConfirmedState extends SimpleCallFlowResponseState {
		//TODO Handle exception
		public void handleTrying(String calleeCallId, ResponseEvent event) {
			
			setState(new CalleeTryingState(), calleeCallId);
		}
		//TODO Handle exception 
		public void handleRinging(String calleeCallId, ResponseEvent event) {
			
			setState(new CalleeRingingState(), calleeCallId);
		}
		
		public void handleOK(String calleeCallId, ResponseEvent event) {
			
			// Check that the dialog of the event is the same as in the session
			// If not this is the result of a fork, we don't handle this
			try {
				Dialog eventDialog = sipUtils.getDialog(event);
				Dialog currentDialog = getDialog(eventDialog.getCallId().getCallId());
				if ( !eventDialog.equals(currentDialog) ) {
					log.warn("Received 200 response from forked dialog");
					return; // We don't currently handle this. Should send ACK and BYE
				}
			} catch ( SipException e ) {
				// TODO Handle this
			}
			
			

			Object content = event.getResponse().getContent();
			String contentString = null;
			if (content instanceof byte[]) {
				contentString = new String((byte[]) content);

			} else if (content instanceof String) {
				contentString = (String) content;
			}

			final byte[] sdpOffer = contentString.getBytes();
			
			// Get the addresses from the sessions
			SessionAssociation sa = (SessionAssociation) cache.get(calleeCallId);
			Session calleeSession = sa.getCalleeSession();			
			Address calleeAddress = calleeSession.getSipAddress();
			// We use the identity of the third party call controller as caller address
			Address callControlAddress = sipUtils.convertURIToAddress(getCallControlSipAddress());
			try {
				callControlAddress.setDisplayName(((SipURI)calleeAddress.getURI()).getUser());
				//callControlAddress.setDisplayName(((SipURI) calleeAddress.getURI()).toString());
			} catch (ParseException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			Session callerSession = sa.getCallerSession();
			Address callerAddress = callerSession.getSipAddress();

			Request request = null;
			try {
				request = sipUtils.buildInvite(callControlAddress, callerAddress, sdpOffer, 1);
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InvalidArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			ClientTransaction ct = null;
			try {
				ct = sipProvider.getNewClientTransaction(request);
			} catch (TransactionUnavailableException e1) {
				e1.printStackTrace();
			}			
			
			// Get the callId of the caller from the transaction
			String callerCallId = ((CallIdHeader) ct.getRequest().getHeader(
					CallIdHeader.NAME)).getCallId();

			// Set the callId for the caller, which was not available until now!
			callerSession.setCallId(callerCallId);
			
			// Store the client transaction in case it needs to be cancelled
			callerSession.setToBeCancelledClientTransaction(ct);
			// Here we obtain the dialog and associate it with the session 
			Dialog dialog = ct.getDialog();
			
			if ( dialog != null && log.isDebugEnabled() ) {
				log.debug("Obtained dialog from ClientTransaction : automatic dialog support on. CallId = " + dialog.getCallId().getCallId());
			}
			if ( dialog == null ) {
				// Automatic dialog support is off
				try {
					dialog = sipProvider.getNewDialog(ct);
					if ( log.isDebugEnabled() ) {
						log.debug("Obtained dialog for INVITE request to caller with getNewDialog. CallId = " + dialog.getCallId().getCallId());
					}	
				} catch (Exception e) {
					log.error("Error getting dialog", e);
				}
			}
			
			callerSession.setDialog(dialog);
			// put the callId for the caller dialog in the cache
			cache.put(callerCallId, sa);
			
		
			// Get activity context from factory
			ActivityContextInterface aci;
			try {
				// 
				aci = activityContextInterfaceFactory.getActivityContextInterface(ct);

				SbbLocalObject mySelf = sbbContext.getSbbLocalObject();
				// Attach child SBB to the activity context
				aci.attach(mySelf);
				ct.sendRequest();

			} catch (FactoryException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (UnrecognizedActivityException e) {
				e.printStackTrace();
			} catch (TransactionRequiredLocalException e) {
				e.printStackTrace();
			} catch (SLEEException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
			setState(new CallerInvitedState(), calleeCallId);
		}
		
		public void handleDecline(String calleeCallId, ResponseEvent event) {
			Dialog dialog = null;
			//TODO Handle exception
			try {
				dialog = sipUtils.getDialog(event);
			} catch (SipException e) {
				log.error("Error getting dialog in handleDecline", e);
			}
			Request ackRequest;
			try {
				ackRequest = dialog.createRequest(Request.ACK);
				dialog.sendAck(ackRequest);
			} catch (SipException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setState(new TerminationState(), calleeCallId);
			getDefaultSbbUsageParameterSet().incrementNumberOfCancelledSessions(1);
		}
		
		
		public void handleAuthentication(String calleeCallId, ResponseEvent event) {
			sendRequestWithAuthorizationHeader(event);
			setState(new InitialState(), calleeCallId);
		}
		
		public abstract void execute(ResponseEvent event);
		
	}

	public class InitialState extends BeforeCalleeConfirmedState {

		public void execute(ResponseEvent event) {
			Response response = event.getResponse();
			int status = response.getStatusCode();
			final String calleeCallId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();
			if (status == Response.TRYING) { 
				// status == 100
				handleTrying(calleeCallId, event);
			} else if (status >= Response.RINGING && status < Response.OK) { 
				// 180 <= status < 200 
				handleRinging(calleeCallId, event);
			} else if (status >= Response.OK && status <= Response.ACCEPTED) { 
				// 200 <= status <= 202
				handleOK(calleeCallId, event);
			} else if (status == Response.DECLINE) {
				handleDecline(calleeCallId, event);
			} else if(status == Response.UNAUTHORIZED || status == Response.PROXY_AUTHENTICATION_REQUIRED) {
				handleAuthentication(calleeCallId, event);
			}
		}
	}

	public class CalleeTryingState extends BeforeCalleeConfirmedState {

		public void execute(ResponseEvent event) {
			Response response = event.getResponse();
			int status = response.getStatusCode();
			final String calleeCallId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();

			if (status >= Response.RINGING && status < Response.OK) { 
				// 180 <= status < 200 
				handleRinging(calleeCallId, event);
			} else if (status >= Response.OK && status <= Response.ACCEPTED) { 
				// 200 <= status <= 202
				handleOK(calleeCallId, event);
			} else if (status == Response.DECLINE) {
				handleDecline(calleeCallId, event);
			} else if(status == Response.UNAUTHORIZED || status == Response.PROXY_AUTHENTICATION_REQUIRED) {
				handleAuthentication(calleeCallId, event);
			}
		}

	}

	public class CalleeRingingState extends BeforeCalleeConfirmedState {

		public void execute(ResponseEvent event) {
			Response response = event.getResponse();
			int status = response.getStatusCode();
			final String calleeCallId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();

			if (status >= Response.OK && status <= Response.ACCEPTED) {
				handleOK(calleeCallId, event);
			} else if (status == Response.DECLINE) {
				handleDecline(calleeCallId, event);
			} else if(status == Response.UNAUTHORIZED || status == Response.PROXY_AUTHENTICATION_REQUIRED) {
				handleAuthentication(calleeCallId, event);
			}
		}

	}

	public abstract class AfterCalleeConfirmedAndBeforeCallerConfirmedState implements SimpleCallFlowState {

		public void handleTrying(String callerCallId, ResponseEvent event) {
			//TODO Handle exception
			
			setState(new CallerTryingState(), callerCallId);
		}
		//TODO Handle exception
		public void handleRinging(String callerCallId, ResponseEvent event) {
			setState(new CallerRingingState(), callerCallId);
		}
		//TODO Handle exception
		public void handleOK(String callerCallId, ResponseEvent event) {
			
			// Check that the dialog of the event is the same as in the session
			// If not this is the result of a fork, we don't handle this
			try {
				Dialog eventDialog = sipUtils.getDialog(event);
				Dialog currentDialog = getDialog(eventDialog.getCallId().getCallId());
				if ( !eventDialog.equals(currentDialog) ) {
					log.warn("Received 200 response from forked dialog");
					return; // We don't currently handle this. Should send ACK and BYE
				}
			} catch ( SipException e ) {
				// TODO Handle this
			}
			
			
			
			sendCalleeAck(event);
			sendCallerAck(event);
			setState(new SessionEstablishedState(), callerCallId);
			getDefaultSbbUsageParameterSet().incrementNumberOfEstablishedSessions(1);
		}
		/**
		 * Used to handle responses in the 400 series.
		 * @param callerCallId
		 */
		public void handleError(String callerCallId) {
			Dialog dialog = getPeerDialog(callerCallId); 
			try {
				sendRequest(dialog, Request.BYE);
			} catch (SipException e) {
				log.error("Error sending BYE", e);
			}
			setState(new UATerminationState(), callerCallId);
		}
		
		
		public void handleDecline(String callerCallId) {
			Dialog dialog = getPeerDialog(callerCallId); 
			try {
				sendRequest(dialog, Request.BYE);
			} catch (SipException e) {
				log.error("Error sending BYE", e);
			}
			setState(new UATerminationState(), callerCallId);
		}
		
		
		public void handleAuthentication(String callerCallId, ResponseEvent event) {
			sendRequestWithAuthorizationHeader(event);
			setState(new CallerInvitedState(), callerCallId);
		}
		/**
		 * Handles a BYE 
		 * @param calleeCallId
		 * @param request
		 */
		public void handleBye(String calleeCallId, Request request) {
			//Send OK to callee
			try {
				sipUtils.sendOk(request);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (SipException e) {
				e.printStackTrace();
			}
			//Send Cancel to caller
			sendRequestCancel(getPeerDialog(calleeCallId));
			setState(new UATerminationState(), calleeCallId);
		}
		
		public abstract void execute(RequestEvent event);
		
		public abstract void execute(ResponseEvent event);
		
	}
	
	public class CallerInvitedState extends AfterCalleeConfirmedAndBeforeCallerConfirmedState {

		public void execute(ResponseEvent event) {
						
			final Response response = event.getResponse();
			final int status = response.getStatusCode();
			final String callerCallId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();

			if (status == Response.TRYING) { // status == 100
				handleTrying(callerCallId, event);
			} else if (status >= Response.RINGING && status < Response.OK) {
				// 180 <= status < 200 
				handleRinging(callerCallId, event);
			} else if (status >= Response.OK && status <= Response.ACCEPTED) {
				// 200 <= status <= 202
				handleOK(callerCallId, event);
			} else if(status == Response.UNAUTHORIZED || status == Response.PROXY_AUTHENTICATION_REQUIRED) {
				handleAuthentication(callerCallId, event);
			} else if (status >= Response.BAD_REQUEST
					&& status < Response.SERVER_INTERNAL_ERROR) { 
				// 400 <= status < 500
				handleError(callerCallId);
			} else if (status == Response.DECLINE) {
				// Status = 603
				handleDecline(callerCallId);
			} 
		}

		public void execute(RequestEvent event) {
			Request request = event.getRequest();
			final String calleeCallId = ((CallIdHeader) request
					.getHeader(CallIdHeader.NAME)).getCallId();
			String method = request.getMethod();
			
			if(Request.BYE.equals(method)) {
				handleBye(calleeCallId, request);
			}
		}
	}

	public class CallerTryingState extends AfterCalleeConfirmedAndBeforeCallerConfirmedState {

		public void execute(ResponseEvent event) {
			final Response response = event.getResponse();
			final int status = response.getStatusCode();
			final String callerCallId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();

			if (status >= Response.RINGING && status < Response.OK) {
				// 180 <= status < 200 
				handleRinging(callerCallId, event);
			} else if (status >= Response.OK && status <= Response.ACCEPTED) {
				// 200 <= status <= 202
				handleOK(callerCallId, event);
			} else if(status == Response.UNAUTHORIZED || status == Response.PROXY_AUTHENTICATION_REQUIRED) {
				handleAuthentication(callerCallId, event);
			} else if (status >= Response.BAD_REQUEST
					&& status < Response.SERVER_INTERNAL_ERROR) { 
				// 400 <= status < 500
				handleError(callerCallId);
			} else if (status == Response.DECLINE) {
				// Status = 603
				handleDecline(callerCallId);
			} 
		}

		public void execute(RequestEvent event) {
			Request request = event.getRequest();
			final String calleeCallId = ((CallIdHeader) request
					.getHeader(CallIdHeader.NAME)).getCallId();
			String method = request.getMethod();
			
			if(Request.BYE.equals(method)) {
				handleBye(calleeCallId, request);
			}
		}

	}

	public class CallerRingingState extends AfterCalleeConfirmedAndBeforeCallerConfirmedState{

		public void execute(ResponseEvent event) {
			final Response response = event.getResponse();
			final int status = response.getStatusCode();
			final String callerCallId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();

			if (status >= Response.OK && status <= Response.ACCEPTED) {
				// 200 <= status <= 202
				handleOK(callerCallId, event);
			} else if(status == Response.UNAUTHORIZED || status == Response.PROXY_AUTHENTICATION_REQUIRED) {
				handleAuthentication(callerCallId, event);
			} else if (status >= Response.BAD_REQUEST
					&& status < Response.SERVER_INTERNAL_ERROR) { 
				// 400 <= status < 500
				handleError(callerCallId);
			} else if (status == Response.DECLINE) {
				// Status = 603
				handleDecline(callerCallId);
			} 
		}

		public void execute(RequestEvent event) {
			Request request = event.getRequest();
			final String calleeCallId = ((CallIdHeader) request
					.getHeader(CallIdHeader.NAME)).getCallId();
			String method = request.getMethod();
			
			if(Request.BYE.equals(method)) {
				handleBye(calleeCallId, request);
			}
		}
	}

	public class SessionEstablishedState extends SimpleCallFlowRequestState {

		public void execute(RequestEvent event) {
			Request request = event.getRequest();
			final String method = request.getMethod();
						
			if (method.equals(Request.BYE)) {
				final String callId = ((CallIdHeader) request
						.getHeader(CallIdHeader.NAME)).getCallId();
				
				Dialog dialog = getPeerDialog(callId);
				try {
					sipUtils.sendOk(request);
					sendRequest(dialog, Request.BYE);
				} catch (ParseException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (SipException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				setState(new UATerminationState(), callId);
			}
		}
	}

	public class UATerminationState extends SimpleCallFlowResponseState {
		public void execute(ResponseEvent event) {
			Response response = event.getResponse();
			int status = response.getStatusCode();
			final String callId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();

			if (status == Response.OK) { // status == 200
				setState(new TerminationState(), callId);
				getDefaultSbbUsageParameterSet().incrementNumberOfTerminatedSessions(1);
			}
		}
	}
	
	/**
	 * We enter this state when an external cancellation has been triggered
	 * (e.g. by invocation of cancel(String guid) method ) and CANCEL has been
	 * sent to calleee.
	 * 
	 */
	public class ExternalCancellationState extends SimpleCallFlowResponseState {
		public void execute(ResponseEvent event) {
			Response response = event.getResponse();
			// We expect a 200 OK 
			// However, we should send switch state to TerminationState whatever the response is 
			// so we simply ignore the response for now. 
			final String callId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();
			setState(new TerminationState(), callId);
		}
	}
	
	/**
	 * We enter this state when the termination of both sessions has been 
	 * triggered (e.g. by invocation of terminate(String guid) method ) and
	 * BYE has been sent to callee. 
	 * @author niklas
	 *
	 */
	public class ExternalTerminationCalleeState extends SimpleCallFlowResponseState {
		public void execute(ResponseEvent event) {
			Response response = event.getResponse();
			// We expect a 200 OK and send a bye to Caller 
			// However, we should send BYE to the Caller whatever the response is 
			// so we simply ignore the response for now. 
			final String callId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();
			
			Dialog dialog = getPeerDialog(callId);
			// TODO Handle exception better (exception indicated in termination state)
				try {
					sendRequest(dialog, Request.BYE);
					setState(new ExternalTerminationCallerState(), callId);
				} catch (SipException e) {
					log.error("Exception while sending BYE in execute for callId : " + dialog.getCallId().getCallId());
					setState(new TerminationState(), callId);
				}
			
		}
	}
	/**
	 * We enter this state when the termination of both sessions has been 
	 * triggered (e.g. by invocation of terminateDialogs(String guid) ), BYE
	 * has been sent to both parties and we wait for a response for the Caller.
	 * <br>
	 * When this arrives the session has been successfully teared down.
	 * @author niklas
	 *
	 */
	public class ExternalTerminationCallerState extends SimpleCallFlowResponseState {
		public void execute(ResponseEvent event) {
			Response response = event.getResponse();
			// We expect a 200 OK but move to TerminationState no matter
			// what the response status is.
			
			//TODO Introduce a termination cause property on the 
			// TerminationEvent to communicate this information 
			final String callId = ((CallIdHeader) response
					.getHeader(CallIdHeader.NAME)).getCallId();
			setState(new TerminationState(), callId);
			getDefaultSbbUsageParameterSet().incrementNumberOfTerminatedSessions(1);
		}
	}
	
	public class TerminationState extends SimpleCallFlowResponseState {

		public void execute(ResponseEvent event) {
			// FINAL state, do nothing!
		}

	}

	/**
	 * Associates a state object to the association of the two sip dialogs
	 * defining the unit of call control.
	 * 
	 * @param state
	 * @param callId
	 */
	private void setState(SimpleCallFlowState state, String callId) {
		if( log.isDebugEnabled() ) {
			log.debug("Setting state to " + state + " for callId " + callId);;
		}
		String stateName = state.getClass().getName();
		SessionAssociation sa = (SessionAssociation) cache.get(callId);
		// This state is used to manage the state machine transitions in the Sbb implementation
		sa.setState(stateName);
		// Also communicate the state to external observers
		StateCallback callback = sa.getStateCallback();
		if ( callback != null ) { // The callback property is optional and can be null
			callback.setSessionState(stateName);
		}
		
	}


	/**
	 * Utility method that associates a Dialog to the Session it belongs to via
	 * its callID. <br>
	 * <br>
	 * Note: This is a hack that needs to be fixed. There can be multiple
	 * dialogs that arise as a consequence of an invite being sent. This must be
	 * dealt with.
	 * 
	 * 
	 * @param dialog
	 * @param callId
	 */
	// TODO Fix the hack referred to above
	private void setDialog(Dialog dialog, String callId) {
		SessionAssociation sa = (SessionAssociation) cache.get(callId);
		Session session = sa.getSession(callId);
		if (log.isDebugEnabled()) {
			log.debug("Setting dialog in session for callId : " + callId);
		}
		session.setDialog(dialog);
	}

	

	public void sendCallerAck(ResponseEvent event) {
		try {
			Dialog dialog = sipUtils.getDialog(event);
			Request ackRequest = sipUtils.buildAck(dialog, null);
			dialog.sendAck(ackRequest);
		} catch (SipException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Accepts a response event and sends an ACK (containing the sdp from this
	 * event) to the callee.
	 * 
	 * @param event
	 */
	private void sendCalleeAck(ResponseEvent event) {
		try {
			ClientTransaction ct = event.getClientTransaction();
			final String callerCallId = ((CallIdHeader) ct.getRequest().getHeader(
					CallIdHeader.NAME)).getCallId();

			Dialog calleeDialog = getPeerDialog(callerCallId);
			Object content = event.getResponse().getContent();
			Request ackRequest = sipUtils.buildAck(calleeDialog, content);
			calleeDialog.sendAck(ackRequest);
			
		} catch (SipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Dialog getPeerDialog(String callId) {
		SessionAssociation sa = (SessionAssociation) cache.get(callId);
		Session peerSession = sa.getPeerSession(callId);
		return peerSession.getDialog();
	}

	private Dialog getDialog(String callId) {
		SessionAssociation sa = (SessionAssociation) cache.get(callId);
		Session session = sa.getSession(callId);
		return session.getDialog();
	}
	
	/**
	 * The cancel request uses the same sequence number as the 
	 * invite it is ment to cancel and thus needs to be treated
	 * specially. I.e. retrive the sequence number
	 * from this invite and use this in the new cancel request.
	 * 
	 * @param dialog The dialog of the UA where the cancel is to be sent.
	 */
	private void sendRequestCancel(Dialog dialog) {
		//Send to callee	
		try {
			//Retrieve the client transacation to cancel			
			Session session = getSession(dialog);
			ClientTransaction ct = session.getToBeCancelledClientTransaction();
			sipUtils.sendCancel(ct);
		} catch (SipException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Send a request to a UA. 
	 * 
	 * @param dialog The dialog of the UA where the request is to be sent.
	 * @param requestType The request type to send to the UA. 
	 */
	private void sendRequest(Dialog dialog, String requestType) throws SipException{
		Request request;
		request = dialog.createRequest(requestType);
		
		
		/* If the request is of type BYE, modify the via and
		   contact header to match the expected behavior. I.e. 
		   due to a bug in jain sip, the contact header is incorrectly set to 
		   the address of the receiver. This header should instead
		   point to the address running this application. The bug
		   has been reported! 
		*/ 
		if(Request.BYE.equals(requestType)) {
			//TODO Do we have to change the VIA header?
			request.removeHeader(ViaHeader.NAME);
			request.removeHeader(ContactHeader.NAME);
			
			try {
				request.addHeader(sipUtils.createLocalViaHeader());
				request.addHeader(sipUtils.createLocalContactHeader());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		// Get a client transaction and corresponding activity context interface
		// Attach ourselves to receive responses and finally send the request
		try {
			ClientTransaction ct = sipProvider.getNewClientTransaction(request);
			ActivityContextInterface acIntf = activityContextInterfaceFactory.getActivityContextInterface(ct);
			SbbLocalObject mySelf = sbbContext.getSbbLocalObject();
			acIntf.attach(mySelf);
			dialog.sendRequest(ct);
		} catch (Exception e) { // This catches no less than 10 distinct exception types... 
			log.error("Exception in sendrequest", e);
			throw new SipException("Exception rethrown as SipException in sendRequest", e);
		}
	}
	
	/**
	 * Send the request associated with the event again but this time include an authorization header
	 * based on the Digest Scheme presented in RFC 2069. 
	 * 
	 * @param event
	 * @param password
	 */
	
	private void sendRequestWithAuthorizationHeader(ResponseEvent event) {

		ClientTransaction ct = null;
		try {
			Request request = sipUtils.buildRequestWithAuthorizationHeader(event, getPassword());
			ct = sipProvider.getNewClientTransaction(request);
		} catch (TransactionUnavailableException e) {
			e.printStackTrace();
		}
		
		Dialog dialog = ct.getDialog();
		
		
		//TODO Handle exception
		if ( dialog == null ) {
			// Automatic dialog support is off
			try {
				dialog = sipProvider.getNewDialog(ct);
				if ( log.isDebugEnabled() ) {
					log.debug("Obtained dialog with getNewDialog in sendRequestWithAuthorizationHeader");
				}
			} catch (SipException e) {
				log.error("Error getting dialog in sendRequestWithAuthorizationHeader", e);
			}
		}
		
		final String callId = dialog.getCallId().getCallId();
		
		if ( log.isDebugEnabled() ) {
			log.debug("Obtained dialog from ClientTransaction in sendRequestWithAuthorizationHeader " +
			" : dialog callId = " + callId );
		}
		//Store the client transaction in the cache
		//since we may need to send a cancel request associated with this
		//ClientTransaction later.
		setToBeCancelledClientTransaction(ct);
						
		//Store the dialog in the cache with the associated call id of the new transaction. 
		setDialog(dialog, callId);
		
		//Finally, send the request!
		try {
			ct.sendRequest();
		} catch (SipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//Since we are about the send the request in a new client transaction, we need to 
		//attach it to the new activity context in order to receive the following events.
		//We start this by getting the activity context
		ActivityContextInterface ac = null;
		try {
			ac = activityContextInterfaceFactory.getActivityContextInterface(ct);
		} catch (FactoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecognizedActivityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Attach our local interface to the new ActivityContextInterface
		// This makes this Sbb receive responses to the request
		SbbLocalObject sbbLocalObject = getSbbContext().getSbbLocalObject();
		ac.attach(sbbLocalObject);
	}
	
	/**
	 * 
	 * @param callId
	 * @return
	 */
	private Session getSession(Dialog dialog) {
		final String callId = dialog.getCallId().getCallId();
		SessionAssociation sa = (SessionAssociation) cache.get(callId);
		Session session = sa.getSession(callId);
		return session;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getCallControlSipAddress() {
		return callControlSipAddress;
	}

	public void setCallControlSipAddress(String username) {
		this.callControlSipAddress = username;
	}

	
}
