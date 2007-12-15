package com.ptin.xmas.control.events;

import java.util.Random;
import java.io.Serializable;

public final class StatusPeerDisconnectedEvent implements Serializable {
    public static int EVENT_CODE = 29;
    
	public StatusPeerDisconnectedEvent() {
		id = new Random().nextLong() ^ System.currentTimeMillis();
	}

	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null) return false;
		return (o instanceof StatusPeerDisconnectedEvent) && ((StatusPeerDisconnectedEvent)o).id == id;
	}
	
	public int hashCode() {
		return (int) id;
	}
	
	public String toString() {
		return "StatusPeerDisconnectedEvent[" + hashCode() + "]";
	}

	private final long id;
}
