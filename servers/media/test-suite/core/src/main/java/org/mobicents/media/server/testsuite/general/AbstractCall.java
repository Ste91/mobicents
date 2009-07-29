/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.testsuite.general;

import jain.protocol.ip.mgcp.message.parms.CallIdentifier;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;

import javax.sdp.Attribute;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;

import org.apache.log4j.Logger;
import org.mobicents.media.server.testsuite.general.rtp.RtpPacket;
import org.mobicents.mgcp.stack.JainMgcpExtendedListener;
import org.mobicents.mgcp.stack.JainMgcpStackProviderImpl;

/**
 * 
 * @author baranowb
 */
public abstract class AbstractCall implements JainMgcpExtendedListener, Runnable, Serializable {

	protected transient Logger logger = Logger.getLogger(AbstractCall.class);
	// We are serializable, just in case.
	public static final int _READ_PERIOD = 20;
	// Some static vals:
	protected transient static final AtomicLong _GLOBAL_SEQ = new AtomicLong(-1);

	protected final long sequence;
	protected CallState state = CallState.INITIAL;
	protected int avgJitter;
	protected int peakJitter;
	protected long lastDeliverTimeStamp;

	// Data dump
	protected transient java.io.File dataFileName;
	protected transient java.io.File graphDataFileName;
	protected transient FileOutputStream fos;
	protected transient ObjectOutputStream dataDumpChannel;
	protected transient FileOutputStream graphDataDumpChannel;
	protected transient List<RtpPacket> rtpTraffic = new ArrayList<RtpPacket>();
	// Media part
	protected String endpointName = ""; // endpoint name with wildcard - used to
	// send to mms to get actual EI
	protected EndpointIdentifier endpointIdentifier;
	protected CallIdentifier callIdentifier;

	// Below is part we dont want to propagate to other side :)
	protected transient AbstractTestCase testCase;
	// RTP part
	protected transient DatagramChannel datagramChannel;
	// protected transient DatagramSocket socket = null;
	protected transient final ScheduledExecutorService readerThread;
	protected transient ScheduledFuture readerTask;
	protected transient boolean receiveRTP;

	// MGCP part
	protected transient JainMgcpStackProviderImpl provider;

	protected transient ScheduledFuture<?> timeoutHandle = null;

	public static void resetSequence() {
		_GLOBAL_SEQ.set(-1);
	}

	public AbstractCall(AbstractTestCase testCase) throws IOException {
		this.testCase = testCase;
		this.callIdentifier = testCase.getProvider().getUniqueCallIdentifier();
		boolean finished = false;
		try {

			this.sequence = _GLOBAL_SEQ.incrementAndGet();
                        this.readerThread = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("CallThreadFactory-"+this.getClass().getName()+"-"+this.sequence));
			this.setDumpDir(this.testCase.getTestDumpDirectory());
			this.fos = new FileOutputStream(dataFileName);
			this.dataDumpChannel = new ObjectOutputStream(fos);
			this.graphDataDumpChannel = new FileOutputStream(this.graphDataFileName);
			this.provider = testCase.getProvider();

			finished = true;
		} finally {
			if (!finished) {
				try {
					if (datagramChannel.isConnected() || datagramChannel.isOpen()) {
						datagramChannel.close();
						// This will close socket.
					}
					this.datagramChannel = null;
				} catch (Exception e) {
				}
				// if (socket != null && (socket.isBound() ||
				// socket.isConnected())) {
				// socket.close();
				// }
				try {
					if (dataDumpChannel != null) {
						dataDumpChannel.close();
					}

					this.dataDumpChannel = null;

					this.fos = null;
				} catch (Exception e) {
					logger.error(e);
				}
				try {
					if (this.graphDataDumpChannel != null) {
						this.graphDataDumpChannel.close();
						this.graphDataDumpChannel = null;
					}
				} catch (Exception e) {
					logger.error(e);
				}

			}

		}
	}

	void setDumpDir(File testDumpDirectory) {
		this.dataFileName = new File(testDumpDirectory, this.sequence + ".rtp");
		this.graphDataFileName = new File(testDumpDirectory, this.sequence + "_graph.txt");
	}

	
	
	public java.io.File getGraphDataFileName() {
		return graphDataFileName;
	}

	protected void initSocket() throws IOException {
		this.datagramChannel = DatagramChannel.open();
		this.datagramChannel.configureBlocking(false);
		DatagramSocket socket = this.datagramChannel.socket();

		InetAddress bindAddress = this.testCase.getClientTestNodeAddress();
		// this.socket = new DatagramSocket();
		for (int i = 1024; i < 65535; i++) {
			try {
				SocketAddress address = new InetSocketAddress(bindAddress, i);
				// socket = new DatagramSocket(address);
				socket.bind(address);
				logger.debug("Socket created port = " + socket.getLocalPort() + " address = " + socket.getLocalAddress());
				return;
			} catch (SocketException e) {
				// e.printStackTrace();
				continue;
			}
		}
		throw new SocketException();
	}

	public List<RtpPacket> getRtp() throws IOException {
		List<RtpPacket> ll = this.loadRtp();

		return ll;
	}

	private List<RtpPacket> loadRtp() throws IOException {
		ArrayList<RtpPacket> list = new ArrayList();
		FileInputStream fin = new FileInputStream(this.dataFileName);

		ObjectInputStream in = new ObjectInputStream(fin);

		// This is weird
		while (fin.available() > 0) {
			// while(in.available()>0) {
			try {
				RtpPacket p = (RtpPacket) in.readObject();
				list.add(p);
			} catch (ClassNotFoundException e) {
			}
		}
		return list;
	}

	public CallState getState() {
		return state;
	}

	public int getAvgJitter() {
		return this.avgJitter;
	}

	public int getPeakJitter() {
		return this.peakJitter;
	}

	public EndpointIdentifier getEndpoint() {
		return this.endpointIdentifier;
	}

	public CallIdentifier getCallID() {
		return this.callIdentifier;
	}

	public long getSequence() {
		return this.sequence;
	}

	protected void setState(CallState state) {
		if (logger.isDebugEnabled()) {
			logger.debug("Dumping data to file. State = " + state);
		}
		if (state == this.state) {
			return;
		}

		this.state = state;

		switch (this.state) {
		case ENDED:
		case IN_ERROR:
		case TIMED_OUT:
			this.receiveRTP = false;
			try {
				if (datagramChannel != null && (datagramChannel.isConnected() || datagramChannel.isOpen())) {
					datagramChannel.close();
				}
				this.datagramChannel = null;
			} catch (Exception e) {
			}

			if (rtpTraffic != null && rtpTraffic.size()>0) {
//				try {
//					if (graphDataDumpChannel != null) {
//						graphDataDumpChannel.write(("Call ID: "+this.getCallID()+AbstractTestCase._LINE_SEPARATOR).getBytes());
//					}
//				} catch (IOException ex) {
//					logger.error(ex);
//				}
				
				for (int i = 0; i < rtpTraffic.size() - 1; i++) {
					RtpPacket p1 = rtpTraffic.get(i);
					RtpPacket p2 = rtpTraffic.get(i + 1);
					int localJitter = (int) (p2.getTime().getTime() - p1.getTime().getTime());
					if(localJitter> this.peakJitter)
					 this.peakJitter = localJitter;
					//Aprox
					this.avgJitter+=localJitter;
					this.avgJitter/=2;
					
				}
				try {
					if (graphDataDumpChannel != null) {
						graphDataDumpChannel.write((sequence + AbstractTestCase._LINE_SEPARATOR).getBytes());
						graphDataDumpChannel.write((avgJitter + AbstractTestCase._LINE_SEPARATOR).getBytes());
						graphDataDumpChannel.write((peakJitter + AbstractTestCase._LINE_SEPARATOR).getBytes());
					}
					
				} catch (IOException ex) {
					logger.error(ex);
				}
				
				for (int i = 0; i < rtpTraffic.size() - 1; i++) {
					RtpPacket p1 = rtpTraffic.get(i);
					RtpPacket p2 = rtpTraffic.get(i + 1);
					// FIXME: Oleg should we add filler packets with "zero"
					// timestamp when sequence gap is detected?
					try {
						if (dataDumpChannel != null) {
							dataDumpChannel.writeObject(p1);
						}
					} catch (IOException ex) {
						logger.error(ex);
					}
					try {
						if (graphDataDumpChannel != null) {
							int localJitter = (int) (p2.getTime().getTime() - p1.getTime().getTime());
							
							
							graphDataDumpChannel.write((localJitter + AbstractTestCase._LINE_SEPARATOR).getBytes());
						}
						
					} catch (IOException ex) {
						logger.error(ex);
					}

				}
				try {
					if (dataDumpChannel != null) {
						RtpPacket p1 = rtpTraffic.get(rtpTraffic.size()-1);
						dataDumpChannel.writeObject(p1);
					}
				} catch (IOException ex) {
					logger.error(ex);
				}
				this.rtpTraffic.clear();
			}
			// for (int index = 0; index < this.rtpTraffic.size(); index++) {
			// // for(RtpPacket packet:this.rtpTraffic){
			// RtpPacket packet = this.rtpTraffic.remove(0);
			// // logger.info("Dumping data to file: "+packet);
			//
			// try {
			// if (dataDumpChannel != null) {
			// dataDumpChannel.writeObject(packet);
			// }
			// } catch (IOException ex) {
			// logger.error(ex);
			// }
			// }
			// This is cause here we get like 60 packets, dunno why....
			
			this.rtpTraffic = null;

			// receiveRTP = false;
			// if (socket != null && (socket.isBound() || socket.isConnected()))
			// {
			// socket.close();
			// }
			try {
				if (dataDumpChannel != null) {
					dataDumpChannel.close();
				}
				this.dataDumpChannel = null;

				this.fos = null;
			} catch (Exception e) {
			}
			try {
				if (this.graphDataDumpChannel != null) {
					this.graphDataDumpChannel.close();
					this.graphDataDumpChannel = null;
				}
			} catch (Exception e) {
				logger.error(e);
			}finally
                        {
                            if(this.readerThread!=null)
                            {
                                this.readerThread.shutdownNow();
                            }
                        }
			break;

		default:
			break;
		}

		this.testCase.callStateChanged(this);

	}

	protected String getLocalDescriptor(int port) {

		SessionDescription localSDP = null;
		String userName = "Mobicents-Call-Generator";
		long sessionID = System.currentTimeMillis() & 0xffffff;
		long sessionVersion = sessionID;

		String networkType = javax.sdp.Connection.IN;
		String addressType = javax.sdp.Connection.IP4;

		SdpFactory sdpFactory = testCase.getSdpFactory();

		try {
			localSDP = sdpFactory.createSessionDescription();
			localSDP.setVersion(sdpFactory.createVersion(0));
			localSDP.setOrigin(sdpFactory.createOrigin(userName, sessionID, sessionVersion, networkType, addressType, this.testCase.getClientTestNodeAddress().getHostAddress()));
			localSDP.setSessionName(sdpFactory.createSessionName("session"));
			localSDP.setConnection(sdpFactory.createConnection(networkType, addressType, this.testCase.getClientTestNodeAddress().getHostAddress()));

			Vector<Attribute> attributes = testCase.getSDPAttributes();
			int[] audioMap = new int[attributes.size()];
			for (int index = 0; index < audioMap.length; index++) {
				String m = attributes.get(index).getValue().split(" ")[0];
				audioMap[index] = Integer.valueOf(m);
			}
			// generate media descriptor
			MediaDescription md = sdpFactory.createMediaDescription("audio", port, 1, "RTP/AVP", audioMap);

			// set attributes for formats

			md.setAttributes(attributes);
			Vector descriptions = new Vector();
			descriptions.add(md);

			localSDP.setMediaDescriptions(descriptions);
		} catch (SdpException e) {
			e.printStackTrace();
		}

		// System.out.println("Local SDP: " + localSDP.toString());

		return localSDP.toString();
	}

	// Run method for read, we are run in readerThread
	public void run() {
		try {
			if (receiveRTP) {

				// byte[] buffer = new byte[172];
				// DatagramPacket packet = new DatagramPacket(buffer,
				// buffer.length);
				ByteBuffer packetBuffer = ByteBuffer.allocate(172);
				try {
					// socket.receive(packet);
					long received = 0;
					int readCount = 0;
					int readMaxCount = 4;
					while (received != 172 && readCount < readMaxCount) {
						received += datagramChannel.read(packetBuffer);
						readCount++;
					}

					// RtpPacket rtp = new RtpPacket(packetBuffer.array());

					packetBuffer.flip();
					RtpPacket rtp = new RtpPacket(packetBuffer.array());
					rtp.setTime(new Date(System.currentTimeMillis()));

					
					if (rtpTraffic != null) {
						rtpTraffic.add(rtp);
					} else {

					}

					// FIXME: add calcualtion of jiiter stuff
				} catch (SocketException e) {
					// This will happen on read, as we odnt sync it can be that
					// flag receiveRTP is fale but read thread is already in
					// while block.
					e.printStackTrace();

				} catch (IOException e) {
					e.printStackTrace();
				} catch (java.nio.channels.NotYetConnectedException e) {
					e.printStackTrace();
				}
			}

		} finally {

		}
	}

	public abstract void start();

	public abstract void timeOut();

	public abstract void stop();

	public ScheduledFuture<?> getTimeoutHandle() {
		return timeoutHandle;
	}

	public void setTimeoutHandle(ScheduledFuture<?> timeoutHandle) {
		this.timeoutHandle = timeoutHandle;
	}

}
