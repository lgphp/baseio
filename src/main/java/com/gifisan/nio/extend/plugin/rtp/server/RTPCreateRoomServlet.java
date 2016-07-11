package com.gifisan.nio.extend.plugin.rtp.server;

import com.gifisan.nio.component.Session;
import com.gifisan.nio.component.future.nio.NIOReadFuture;

public class RTPCreateRoomServlet extends RTPServlet {

	public static final String	SERVICE_NAME	= RTPCreateRoomServlet.class.getSimpleName();

	public void accept(Session session, NIOReadFuture future, RTPSessionAttachment attachment) throws Exception {

		RTPRoom room = attachment.createRTPRoom(session);

		future.write(String.valueOf(room.getRoomID()));

		session.flush(future);
	}

}
