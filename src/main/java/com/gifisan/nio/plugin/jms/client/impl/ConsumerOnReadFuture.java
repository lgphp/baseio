package com.gifisan.nio.plugin.jms.client.impl;

import com.gifisan.nio.client.ClientSession;
import com.gifisan.nio.client.OnReadFuture;
import com.gifisan.nio.component.future.ReadFuture;
import com.gifisan.nio.plugin.jms.JMSException;
import com.gifisan.nio.plugin.jms.Message;
import com.gifisan.nio.plugin.jms.client.OnMessage;
import com.gifisan.nio.plugin.jms.decode.DefaultMessageDecoder;
import com.gifisan.nio.plugin.jms.decode.MessageDecoder;

public class ConsumerOnReadFuture implements OnReadFuture {

	private OnMessage		onMessage	= null;
	private MessageDecoder	messageDecoder	= new DefaultMessageDecoder();

	public ConsumerOnReadFuture(OnMessage onMessage) {
		this.onMessage = onMessage;
	}

	public void onResponse(ClientSession session, ReadFuture future) {
		try {
			
			Message message = messageDecoder.decode(future);
			
			onMessage.onReceive(message);
			
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
}