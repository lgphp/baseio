package com.gifisan.nio.extend.plugin.jms.server;

import com.gifisan.nio.component.Session;
import com.gifisan.nio.component.future.nio.NIOReadFuture;

public class MQConsumerServlet extends MQServlet {

	public static final String	SERVICE_NAME	= MQConsumerServlet.class.getSimpleName();

	public void accept(Session session, NIOReadFuture future, MQSessionAttachment attachment) throws Exception {
		
		getMQContext().pollMessage(session, future, attachment);
	}
}
