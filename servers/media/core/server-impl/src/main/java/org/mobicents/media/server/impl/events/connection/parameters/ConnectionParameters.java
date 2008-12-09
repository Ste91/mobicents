package org.mobicents.media.server.impl.events.connection.parameters;

import org.mobicents.media.server.impl.AbstractSignal;
import org.mobicents.media.server.impl.BaseConnection;
import org.mobicents.media.server.impl.BaseEndpoint;
import org.mobicents.media.server.impl.Generator;
import org.mobicents.media.server.impl.events.announcement.AudioPlayer;


public class ConnectionParameters extends AbstractSignal {

	@Override
	public void apply(BaseConnection connection) {
		
		BaseEndpoint endpoint = (BaseEndpoint) connection.getEndpoint();
        ConnectionParametersGenerator player = (ConnectionParametersGenerator) endpoint.getMediaSource(Generator.CONNECTION_PARAMETERS, connection);
        //now lets hit players button
		player.generateParametersEvent(connection);
	}

	@Override
	public void apply(BaseEndpoint endpoint) {
		
		
	}

}
