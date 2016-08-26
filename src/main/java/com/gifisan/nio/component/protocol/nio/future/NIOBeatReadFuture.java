package com.gifisan.nio.component.protocol.nio.future;

import java.io.IOException;

import com.gifisan.nio.component.Parameters;
import com.gifisan.nio.component.Session;
import com.gifisan.nio.component.protocol.AbstractIOReadFuture;

public class NIOBeatReadFuture extends AbstractIOReadFuture implements NIOReadFuture {

	public NIOBeatReadFuture(Session session) {
		super(session);
		this.isBeatPacket = true;
	}

	public boolean read() throws IOException {
		return true;
	}

	public String getServiceName() {
		return null;
	}

	public Integer getFutureID() {
		return null;
	}

	public String getText() {
		return null;
	}

	public Parameters getParameters() {
		return null;
	}

	public int getStreamLength() {
		return 0;
	}
	
}
