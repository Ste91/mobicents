package org.mobicents.media.server.impl.resource.ivr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.Format;
import org.mobicents.media.server.ConnectionFactory;
import org.mobicents.media.server.EndpointImpl;
import org.mobicents.media.server.impl.clock.TimerImpl;
import org.mobicents.media.server.impl.dsp.DspFactory;
import org.mobicents.media.server.impl.dsp.audio.g711.ulaw.DecoderFactory;
import org.mobicents.media.server.impl.dsp.audio.g711.ulaw.EncoderFactory;
import org.mobicents.media.server.impl.resource.DemuxFactory;
import org.mobicents.media.server.impl.resource.MuxFactory;
import org.mobicents.media.server.impl.resource.audio.AudioPlayerEvent;
import org.mobicents.media.server.impl.resource.audio.AudioPlayerFactory;
import org.mobicents.media.server.impl.resource.audio.RecorderEvent;
import org.mobicents.media.server.impl.resource.audio.RecorderFactory;
import org.mobicents.media.server.impl.resource.dtmf.Rfc2833DetectorFactory;
import org.mobicents.media.server.impl.resource.dtmf.Rfc2833GeneratorFactory;
import org.mobicents.media.server.impl.rtp.RtpFactory;
import org.mobicents.media.server.impl.rtp.sdp.AVProfile;
import org.mobicents.media.server.resource.ChannelFactory;
import org.mobicents.media.server.resource.PipeFactory;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.Timer;
import org.mobicents.media.server.spi.events.NotifyEvent;
import org.mobicents.media.server.spi.resource.AudioPlayer;
import org.mobicents.media.server.spi.resource.DtmfDetector;
import org.mobicents.media.server.spi.resource.DtmfGenerator;
import org.mobicents.media.server.spi.resource.Recorder;

/**
 * 
 * @author amit bhayani
 * 
 */
public class IvrTest {

    private int localPort1 = 9201;
    private int localPort2 = 9202;
    
    private Timer timer;
    private EndpointImpl sender;
    private EndpointImpl ivrEnp;
    private RtpFactory rtpFactory1, rtpFactory2;
    private Rfc2833DetectorFactory rfc2833fact;
    private DemuxFactory deMuxFact;
    private RecorderFactory recFact;
    private ChannelFactory rxChannFact;
    private Semaphore semaphore;
    private boolean started = false;
    private boolean end_of_media = false;
    private boolean failed = false;
    private boolean stopped = false;
    private boolean completed = false;
    private boolean receivedEvent = false;

    public IvrTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {

        timer = new TimerImpl();

        HashMap<Integer, Format> rtpmap = new HashMap();
        rtpmap.put(0, AVProfile.PCMU);
        rtpmap.put(101, AVProfile.DTMF);

        rtpFactory1 = new RtpFactory();
        rtpFactory1.setBindAddress("localhost");
        rtpFactory1.setLocalPort(localPort1);
        rtpFactory1.setTimer(timer);
        rtpFactory1.setFormatMap(rtpmap);
        rtpFactory1.start();

        rtpFactory2 = new RtpFactory();
        rtpFactory2.setBindAddress("localhost");
        rtpFactory2.setLocalPort(localPort2);
        rtpFactory2.setTimer(timer);
        rtpFactory2.setFormatMap(rtpmap);
        rtpFactory2.start();

        Hashtable<String, RtpFactory> rtpFactories1 = new Hashtable<String, RtpFactory>();
        rtpFactories1.put("audio", rtpFactory1);

        Hashtable<String, RtpFactory> rtpFactories2 = new Hashtable<String, RtpFactory>();
        rtpFactories2.put("audio", rtpFactory2);
        
        // preparing g711: ALaw encoder, ULAW decoder
        EncoderFactory encoderFactory = new EncoderFactory();
        DecoderFactory decoderFactory = new DecoderFactory();

        // group codecs into list
        ArrayList list = new ArrayList();
        list.add(encoderFactory);
        list.add(decoderFactory);

        // creating dsp factory with g711 encoder/decoder
        DspFactory dspFactory = new DspFactory();
        dspFactory.setName("dsp");
        dspFactory.setCodecFactories(list);

        // Create Rfc2833DetectorFactory
        rfc2833fact = new Rfc2833DetectorFactory();
        rfc2833fact.setName("Rfc2833DetectorFactory");

        // Create a Demux Factory
        deMuxFact = new DemuxFactory("DeMux");

        // creating component list
        ArrayList components = new ArrayList();
        components.add(rfc2833fact);
        components.add(deMuxFact);
        components.add(dspFactory);

        // define pipes

        // Exhaust for Rx channel

        PipeFactory p1 = new PipeFactory();
        p1.setInlet(null);
        p1.setOutlet("dsp");

        PipeFactory p2 = new PipeFactory();
        p2.setInlet("DeMux");
        p2.setOutlet("Rfc2833DetectorFactory");

        PipeFactory p3 = new PipeFactory();
        p3.setInlet("dsp");
        p3.setOutlet("DeMux");



        PipeFactory p4 = new PipeFactory();
        p4.setInlet("DeMux");
        p4.setOutlet(null);

        ArrayList pipes = new ArrayList();
        pipes.add(p1);
        pipes.add(p2);
        pipes.add(p3);
        pipes.add(p4);

        rxChannFact = new ChannelFactory();
        rxChannFact.start();

        rxChannFact.setComponents(components);
        rxChannFact.setPipes(pipes);

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setRxChannelFactory(rxChannFact);
        
        // Create RecorderFactory - sink for endpoint
        recFact = new RecorderFactory();
        recFact.setName("RecorderFactory");

        ivrEnp = new EndpointImpl("/ivr/test/dtmf");
        ivrEnp.setSinkFactory(recFact);

        ivrEnp.setTimer(timer);
        ivrEnp.setConnectionFactory(connectionFactory);
        ivrEnp.setRtpFactory(rtpFactories1);

        // start IVREndpoint
        ivrEnp.start();

        /**
         * Components declared for Ann Endpoint
         */        // Mux
        MuxFactory muxFact = new MuxFactory("Mux");

        // Rfc2833Generator
        Rfc2833GeneratorFactory rfc2833GenFact = new Rfc2833GeneratorFactory();
        rfc2833GenFact.setName("Rfc2833GeneratorFactory");

        List annConponents = new ArrayList();
        annConponents.add(muxFact);
        annConponents.add(rfc2833GenFact);
        annConponents.add(dspFactory);

        PipeFactory p5 = new PipeFactory();
        p5.setInlet(null);
        p5.setOutlet("Mux");

        PipeFactory p6 = new PipeFactory();
        p6.setInlet("Rfc2833GeneratorFactory");
        p6.setOutlet("Mux");

        PipeFactory p7 = new PipeFactory();
        p7.setInlet("Mux");
        p7.setOutlet("dsp");

        PipeFactory p8 = new PipeFactory();
        p8.setInlet("dsp");
        p8.setOutlet(null);

        List annPipes = new ArrayList();
        annPipes.add(p5);
        annPipes.add(p6);
        annPipes.add(p7);
        annPipes.add(p8);

        // creating transparent channels
        ChannelFactory channelFactory = new ChannelFactory();
        channelFactory.start();

        channelFactory.setComponents(annConponents);
        channelFactory.setPipes(annPipes);

        ConnectionFactory connectionFactory1 = new ConnectionFactory();
        connectionFactory1.setTxChannelFactory(channelFactory);
        connectionFactory1.setRxChannelFactory(channelFactory);
        
        // creating source
        AudioPlayerFactory genFactory = new AudioPlayerFactory();
        genFactory.setName("test-source");

        // configuring sender
        sender = new EndpointImpl("/ivr/test/sender");
        sender.setTimer(timer);
        sender.setConnectionFactory(connectionFactory1);
        sender.setSourceFactory(genFactory);
        sender.setRtpFactory(rtpFactories2);
        sender.start();

        semaphore = new Semaphore(0);

    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSink method, of class Bridge.
     */
    @Test
    public void testSimpleTransmission() throws Exception {

        // Set-up player
        AudioPlayer player = (AudioPlayer) sender.getComponent("test-source");
        assertNotNull(player);
        player.addListener(new PlayerListener());

        // 13sec audio
        URL url = IvrTest.class.getClassLoader().getResource("org/mobicents/media/server/impl/8kulaw.wav");
        player.setURL(url.toExternalForm());

        // set-up recorder
        Recorder recorder = (Recorder) ivrEnp.getComponent("RecorderFactory");
        assertNotNull(recorder);
        String tempFilePath = url.getPath();
        String recordDir = tempFilePath.substring(0, tempFilePath.lastIndexOf('/'));

        recorder.setRecordDir(recordDir);
        // let us record for 7 sec
        recorder.addListener(new RecorderListener());

        Connection rxConnection = ivrEnp.createConnection(ConnectionMode.RECV_ONLY);

        Connection txConnection = sender.createConnection(ConnectionMode.SEND_ONLY);

        txConnection.setRemoteDescriptor(rxConnection.getLocalDescriptor());
        rxConnection.setRemoteDescriptor(txConnection.getLocalDescriptor());

        DtmfGenerator dtmfGene = (DtmfGenerator) txConnection.getComponent("Rfc2833GeneratorFactory", Connection.CHANNEL_TX);
        assertNotNull(dtmfGene);
        dtmfGene.setDigit("5");

        DtmfDetector dtmfDete = (DtmfDetector) rxConnection.getComponent("Rfc2833DetectorFactory", Connection.CHANNEL_RX);
        assertNotNull(dtmfDete);
        dtmfDete.addListener(new DTMFListener(5));
        dtmfDete.start();

        player.start();
        recorder.setRecordFile("ivr-test/8kulaw.wav");
        recorder.start();

        semaphore.tryAcquire(5, TimeUnit.SECONDS);

        //Fire dtmf
        dtmfGene.start();

        //wait for another few secs
        semaphore.tryAcquire(8, TimeUnit.SECONDS);

        assertTrue(started);
//		assertTrue(completed);
        assertFalse(failed);
        assertFalse(end_of_media);
        assertFalse(stopped);
        assertTrue(receivedEvent);

    }

    private class RecorderListener implements NotificationListener {

        public void update(NotifyEvent event) {
            switch (event.getEventID()) {
                case RecorderEvent.STOPPED:
                    stopped = true;
                    semaphore.release();
                    break;
                case RecorderEvent.DURATION_OVER:
                    completed = true;
                    semaphore.release();
                    break;
                case RecorderEvent.FAILED:
                    failed = true;
                    semaphore.release();
                    break;
            }
        }
    }

    private class PlayerListener implements NotificationListener {

        public void update(NotifyEvent event) {
            switch (event.getEventID()) {
                case AudioPlayerEvent.STARTED:
                    started = true;
                    break;
                case NotifyEvent.COMPLETED:
                    end_of_media = true;
                    semaphore.release();
                    break;
                case AudioPlayerEvent.START_FAILED:
                    failed = true;
                    semaphore.release();
                    break;
            }
        }
    }

    private class DTMFListener implements NotificationListener {

        int eventId = 0;

        public DTMFListener(int eventId) {
            this.eventId = eventId;
        }

        public void update(NotifyEvent event) {
            if (event.getEventID() == eventId) {
                receivedEvent = true;
            }
        }
    }
}
