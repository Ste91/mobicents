package org.mobicents.mgcp.stack;

import jain.protocol.ip.mgcp.JainMgcpCommandEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.AuditEndpoint;
import jain.protocol.ip.mgcp.message.AuditEndpointResponse;
import jain.protocol.ip.mgcp.message.parms.BearerInformation;
import jain.protocol.ip.mgcp.message.parms.CapabilityValue;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.DigitMap;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.InfoCode;
import jain.protocol.ip.mgcp.message.parms.NotifiedEntity;
import jain.protocol.ip.mgcp.message.parms.ReasonCode;
import jain.protocol.ip.mgcp.message.parms.RequestIdentifier;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.message.parms.RestartMethod;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.mobicents.mgcp.stack.parser.MgcpContentHandler;
import org.mobicents.mgcp.stack.parser.MgcpMessageParser;

/**
 * 
 * @author amit bhayani
 * 
 */
public class AuditEndpointHandler extends TransactionHandler {

	private static final Logger logger = Logger.getLogger(AuditEndpointHandler.class);

	private AuditEndpoint command;
	private AuditEndpointResponse response;

	public AuditEndpointHandler(JainMgcpStackImpl stack) {
		super(stack);
	}

	public AuditEndpointHandler(JainMgcpStackImpl stack, InetAddress address, int port) {
		super(stack, address, port);
	}

	@Override
	public JainMgcpCommandEvent decodeCommand(String message) throws ParseException {
		MgcpMessageParser parser = new MgcpMessageParser(new CommandContentHandle());
		try {
			parser.parse(message);
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), -1);
		}

		return command;
	}

	@Override
	public JainMgcpResponseEvent decodeResponse(String message) throws ParseException {

		MgcpMessageParser parser = new MgcpMessageParser(new ResponseContentHandle());
		try {
			parser.parse(message);
		} catch (IOException e) {
			logger.error("Decoding of AUEP Response failed", e);
		}

		return response;
	}

	@Override
	public String encode(JainMgcpCommandEvent event) {

		// encode message header
		AuditEndpoint evt = (AuditEndpoint) event;
		StringBuffer s = new StringBuffer();
		s.append("AUEP ").append(evt.getTransactionHandle()).append(SINGLE_CHAR_SPACE).append(
				evt.getEndpointIdentifier()).append(MGCP_VERSION).append(NEW_LINE);
		// String msg = "AUEP " + evt.getTransactionHandle() + " " +
		// evt.getEndpointIdentifier() + " MGCP 1.0\n";

		// encode mandatory parameters
		InfoCode[] requestedInfos = evt.getRequestedInfo();
		if (requestedInfos != null) {
			s.append("F: ").append(utils.encodeInfoCodeList(requestedInfos));
			// msg += "F: " + utils.encodeInfoCodeList(requestedInfos);
		}

		// return msg;
		return s.toString();
	}

	@Override
	public String encode(JainMgcpResponseEvent event) {
		AuditEndpointResponse response = (AuditEndpointResponse) event;
		ReturnCode returnCode = response.getReturnCode();

		StringBuffer s = new StringBuffer();
		s.append(returnCode.getValue()).append(SINGLE_CHAR_SPACE).append(response.getTransactionHandle()).append(
				SINGLE_CHAR_SPACE).append(returnCode.getComment()).append(NEW_LINE);
		// String msg = returnCode.getValue() + " " +
		// response.getTransactionHandle() + " " + returnCode.getComment()
		// + "\n";
		if (response.getCapabilities() != null) {
			// TODO How to insert a new line with A : for different set of
			// compression Algo?
			s.append("A: ").append(utils.encodeCapabilityList(response.getCapabilities())).append(NEW_LINE);
			// msg += "A:" +
			// utils.encodeCapabilityList(response.getCapabilities()) + "\n";
		}
		if (response.getBearerInformation() != null) {
			s.append("B: ").append(utils.encodeBearerInformation(response.getBearerInformation())).append(NEW_LINE);
			// msg += "B:" +
			// utils.encodeBearerInformation(response.getBearerInformation()) +
			// "\n";
		}
		ConnectionIdentifier[] connectionIdentifiers = response.getConnectionIdentifiers();
		if (connectionIdentifiers != null) {
			s.append("I: ");
			// msg += "I:";
			boolean first = true;
			for (int i = 0; i < connectionIdentifiers.length; i++) {
				if (first) {
					first = false;
				} else {
					s.append(",");
					// msg += ",";
				}
				s.append(connectionIdentifiers[i].toString());
				// msg += connectionIdentifiers[i].toString();
			}
			s.append(NEW_LINE);
			// msg += "\n";
		}
		if (response.getNotifiedEntity() != null) {
			s.append("N: ").append(utils.encodeNotifiedEntity(response.getNotifiedEntity())).append(NEW_LINE);
			// msg += "N:" +
			// utils.encodeNotifiedEntity(response.getNotifiedEntity()) + "\n";
		}
		if (response.getRequestIdentifier() != null) {
			s.append("X: ").append(response.getRequestIdentifier()).append(NEW_LINE);
			// msg += "X:" + response.getRequestIdentifier() + "\n";
		}
		RequestedEvent[] r = response.getRequestedEvents();
		if (r != null) {
			s.append("R: ").append(utils.encodeRequestedEvents(r)).append(NEW_LINE);
			// msg += "R:" + utils.encodeRequestedEvents(r) + "\n";
		}
		EventName[] sEvet = response.getSignalRequests();
		if (sEvet != null) {
			s.append("S: ").append(utils.encodeEventNames(sEvet)).append(NEW_LINE);
			// msg += "S:" + utils.encodeEventNames(s) + "\n";
		}
		if (response.getDigitMap() != null) {
			s.append("D: ").append(response.getDigitMap()).append(NEW_LINE);
			// msg += "D:" + response.getDigitMap() + "\n";
		}
		EventName[] o = response.getObservedEvents();
		if (o != null) {
			s.append("O: ").append(utils.encodeEventNames(o)).append(NEW_LINE);
			// msg += "O:" + utils.encodeEventNames(o) + "\n";
		}
		if (response.getReasonCode() != null) {
			s.append("E: ").append(response.getReasonCode()).append(NEW_LINE);
			// msg += "E:" + response.getReasonCode() + "\n";
		}
		EventName[] t = response.getDetectEvents();
		if (t != null) {
			s.append("T: ").append(utils.encodeEventNames(t)).append(NEW_LINE);
			// msg += "T:" + utils.encodeEventNames(t) + "\n";
		}
		EventName[] es = response.getEventStates();
		if (es != null) {
			s.append("ES: ").append(utils.encodeEventNames(es)).append(NEW_LINE);
			// msg += "ES:" + utils.encodeEventNames(es) + "\n";
		}
		if (response.getRestartMethod() != null) {
			s.append("RM: ").append(response.getRestartMethod()).append(NEW_LINE);
			// msg += "RM:" + response.getRestartMethod() + "\n";
		}
		if (response.getRestartDelay() > 0) {
			s.append("RD: ").append(response.getRestartDelay()).append(NEW_LINE);
			// msg += "RD:" + response.getRestartDelay() + "\n";
		}
		EndpointIdentifier[] z = response.getEndpointIdentifierList();
		if (z != null) {
			s.append("Z: ").append(utils.encodeEndpointIdentifiers(z)).append(NEW_LINE);
			// msg += "Z:" + utils.encodeEndpointIdentifiers(z) + "\n";
		}
		return s.toString();
		// return msg;
	}

	@Override
	public JainMgcpResponseEvent getProvisionalResponse() {
		AuditEndpointResponse provisionalResponse = null;

		if (!sent) {
			provisionalResponse = new AuditEndpointResponse(commandEvent.getSource(),
					ReturnCode.Transaction_Being_Executed);
			provisionalResponse.setTransactionHandle(remoteTID);
		}

		return provisionalResponse;
	}

	private class CommandContentHandle implements MgcpContentHandler {

		public CommandContentHandle() {
		}

		/**
		 * Receive notification of the header of a message. Parser will call
		 * this method to report about header reading.
		 * 
		 * @param header
		 *            the header from the message.
		 */
		public void header(String header) throws ParseException {
			String[] tokens = header.split("\\s");

			String verb = tokens[0].trim();
			String transactionID = tokens[1].trim();
			String version = tokens[3].trim() + " " + tokens[4].trim();

			int tid = Integer.parseInt(transactionID);
			EndpointIdentifier endpoint = utils.decodeEndpointIdentifier(tokens[2].trim());

			command = new AuditEndpoint(getObjectSource(tid), endpoint);
			command.setTransactionHandle(tid);
		}

		/**
		 * Receive notification of the parameter of a message. Parser will call
		 * this method to report about parameter reading.
		 * 
		 * @param name
		 *            the name of the parameter
		 * @param value
		 *            the value of the parameter.
		 */
		public void param(String name, String value) throws ParseException {
			if (name.equalsIgnoreCase("F")) {
				command.setRequestedInfo(utils.decodeInfoCodeList(value));
			} else {
				logger.error("Unknown code " + name);
			}
		}

		/**
		 * Receive notification of the session description. Parser will call
		 * this method to report about session descriptor reading.
		 * 
		 * @param sd
		 *            the session description from message.
		 */
		public void sessionDescription(String sd) throws ParseException {
			throw new ParseException("SessionDescription shouldn't have been included in AUEP command", 0);
		}
	}

	private class ResponseContentHandle implements MgcpContentHandler {

		public ResponseContentHandle() {
		}

		/**
		 * Receive notification of the header of a message. Parser will call
		 * this method to report about header reading.
		 * 
		 * @param header
		 *            the header from the message.
		 */
		public void header(String header) throws ParseException {
			String[] tokens = header.split("\\s");

			int tid = Integer.parseInt(tokens[1]);
			response = new AuditEndpointResponse(stack, utils.decodeReturnCode(Integer.parseInt(tokens[0])));
			response.setTransactionHandle(tid);
		}

		/**
		 * Receive notification of the parameter of a message. Parser will call
		 * this method to report about parameter reading.
		 * 
		 * @param name
		 *            the name of the paremeter
		 * @param value
		 *            the value of the parameter.
		 */
		public void param(String name, String value) throws ParseException {
			if (name.equals("Z")) {
				EndpointIdentifier[] endpointIdentifierList = utils.decodeEndpointIdentifiers(value);
				response.setEndpointIdentifierList(endpointIdentifierList);
			}
			if (name.equalsIgnoreCase("B")) {
				BearerInformation b = utils.decodeBearerInformation(value);
				response.setBearerInformation(b);
			} else if (name.equalsIgnoreCase("I")) {
				ConnectionIdentifier[] is = response.getConnectionIdentifiers();
				if (is == null) {
					ConnectionIdentifier i = new ConnectionIdentifier(value);
					response.setConnectionIdentifiers(new ConnectionIdentifier[] { i });
				} else {
					ArrayList<ConnectionIdentifier> arrayList = new ArrayList<ConnectionIdentifier>();
					Collections.addAll(arrayList, is);
					arrayList.add(new ConnectionIdentifier(value));

					ConnectionIdentifier[] temp = new ConnectionIdentifier[arrayList.size()];
					response.setConnectionIdentifiers(arrayList.toArray(temp));
				}
			} else if (name.equalsIgnoreCase("N")) {
				NotifiedEntity n = utils.decodeNotifiedEntity(value, true);
				response.setNotifiedEntity(n);
			} else if (name.equalsIgnoreCase("X")) {
				RequestIdentifier r = new RequestIdentifier(value);
				response.setRequestIdentifier(r);
			} else if (name.equalsIgnoreCase("R")) {
				RequestedEvent[] r = utils.decodeRequestedEventList(value);
				response.setRequestedEvents(r);

			} else if (name.equalsIgnoreCase("S")) {
				EventName[] s = utils.decodeEventNames(value);
				response.setSignalRequests(s);
			} else if (name.equalsIgnoreCase("D")) {
				DigitMap d = new DigitMap(value);
				response.setDigitMap(d);
			} else if (name.equalsIgnoreCase("O")) {
				EventName[] o = utils.decodeEventNames(value);
				response.setObservedEvents(o);
			} else if (name.equalsIgnoreCase("E")) {
				ReasonCode e = utils.decodeReasonCode(value);
				response.setReasonCode(e);
			} else if (name.equalsIgnoreCase("Q")) {
				// response.set

			} else if (name.equalsIgnoreCase("T")) {
				EventName[] t = utils.decodeEventNames(value);
				response.setDetectEvents(t);
			} else if (name.equalsIgnoreCase("A")) {

				CapabilityValue[] capabilities = response.getCapabilities();
				if (capabilities == null) {
					response.setCapabilities(utils.decodeCapabilityList(value));
				} else {
					CapabilityValue[] newCapability = utils.decodeCapabilityList(value);
					int size = capabilities.length + newCapability.length;
					CapabilityValue[] temp = new CapabilityValue[size];
					int count = 0;
					for (int i = 0; i < capabilities.length; i++) {
						temp[count] = capabilities[i];
						count++;
					}

					for (int j = 0; j < newCapability.length; j++) {
						temp[count] = newCapability[j];
						count++;
					}
					response.setCapabilities(temp);

				}

			} else if (name.equalsIgnoreCase("ES")) {
				EventName[] es = utils.decodeEventNames(value);
				response.setEventStates(es);

			} else if (name.equalsIgnoreCase("RM")) {
				RestartMethod rm = utils.decodeRestartMethod(value);
				response.setRestartMethod(rm);
			} else if (name.equalsIgnoreCase("RD")) {
				int restartDelay = 0;
				try {
					restartDelay = Integer.parseInt(value);
				} catch (NumberFormatException nfe) {
					logger.error("RD throws error " + value, nfe);
				}
				response.setRestartDelay(restartDelay);
			}
		}

		/**
		 * Receive notification of the session description. Parser will call
		 * this method to report about session descriptor reading.
		 * 
		 * @param sd
		 *            the session description from message.
		 */
		public void sessionDescription(String sd) throws ParseException {
			// response.setLocalConnectionDescriptor(new
			// ConnectionDescriptor(sd));
		}
	}

}
