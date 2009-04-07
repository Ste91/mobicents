package test.msgflow.mediamixer;

import java.io.Serializable;

import javax.media.mscontrol.Joinable;
import javax.media.mscontrol.JoinableStream;
import javax.media.mscontrol.JoinableStream.StreamType;
import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.StatusEvent;
import javax.media.mscontrol.StatusEventListener;
import javax.media.mscontrol.Joinable.Direction;
import javax.media.mscontrol.mixer.MediaMixer;
import javax.media.mscontrol.networkconnection.NetworkConnection;
import javax.media.mscontrol.resource.Error;

import org.apache.log4j.Logger;
import org.mobicents.javax.media.mscontrol.MediaSessionImpl;

import test.msgflow.MessageFlowHarness;

/**
 * 
 * @author amit bhayani
 * 
 */
public class MediaMixerTest extends MessageFlowHarness implements Serializable {

	private MGW mgw;

	public MediaMixerTest() {
		super();
		logger = Logger.getLogger(MediaMixerTest.class);
	}

	public MediaMixerTest(String name) {
		super(name);
		logger = Logger.getLogger(MediaMixerTest.class);
	}

	public void setUp() {
		try {
			super.setUp();
			mgw = new MGW(mgwProvider);

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected Exception");
		}
	}

	// TODO : Should repeat all test of NC?

	public void testMediaMixerJoin() throws Exception {
		final MediaSessionImpl myMediaSession = (MediaSessionImpl) msControlFactory.createMediaSession();
		final NetworkConnection NC1 = myMediaSession.createNetworkConnection();

		final NetworkConnection NC2 = myMediaSession.createNetworkConnection();
		final NetworkConnection NC3 = myMediaSession.createNetworkConnection();

		final MediaMixer MX1 = myMediaSession.createMediaMixer();

		StatusEventListener statusEvtList = new StatusEventListener() {
			int noOfJoins = 0;

			public void onEvent(StatusEvent event) {
				if (event.getError().equals(Error.e_OK)) {
					logger.info("testMediaMixerJoin successful ");
					noOfJoins++;
					if (noOfJoins == 3) {
						logger.info("Join successful " + event);
						testPassed = true;
					}

				} else {
					logger.error("Join failed " + event);
					fail("Join of NC1 and NC2 failed");
				}
			}
		};

		MX1.addListener(statusEvtList);
		MX1.joinInitiate(Direction.DUPLEX, NC1, this);
		MX1.joinInitiate(Direction.DUPLEX, NC2, this);
		MX1.joinInitiate(Direction.DUPLEX, NC3, this);
		waitForMessage();

		/*
		 * Test for COntainers
		 */
		// Get other container
		Joinable[] otherContainer = MX1.getJoinees();
		assertEquals(3, otherContainer.length);

		// MX1 <---> Send / Recv <---> NC1
		otherContainer = MX1.getJoinees(Direction.SEND);
		assertEquals(3, otherContainer.length);

		// MX1 is joined to NC1 Container with direction SEND / RECV and NC1
		// joinees is
		// equal to MX1
		assertEquals(MX1, (otherContainer[0].getJoinees(Direction.RECV))[0]);

		/*
		 * Test for Audio Stream
		 */
		// Joinable Stream is of type Audio only
		JoinableStream stream = MX1.getJoinableStream(StreamType.audio);
		assertNotNull(stream);
		assertNull(MX1.getJoinableStream(StreamType.video));

		// Joined to 3 other NC AudioStream
		Joinable[] j = stream.getJoinees();
		assertEquals(3, j.length);

		// The stream from this MX and its joinees joinees will be same
		JoinableStream streamOther = (JoinableStream) j[0];
		Joinable[] j1 = streamOther.getJoinees();

		assertEquals(j1[0], stream);
		assertNotNull(streamOther.getJoinees(Direction.RECV)[0]);

		NetworkConnection NC4 = myMediaSession.createNetworkConnection();
		// NC1 is not joined to NC3. Trying to unjoin NC3 should raise exception
		try {
			MX1.unjoinInitiate(NC4, this);
			fail("MX1 not connected to NC4");
		} catch (MsControlException e) {
			// expected
			// e.printStackTrace();
		}

		assertTrue(this.getName() + " passed = " + testPassed, testPassed);
	}

	private class ContextImpl implements Serializable {

	}

	public void tearDown() {
		this.mgw.checkState();
		try {
			super.tearDown();
		} catch (Exception ex) {
		}
	}

}
