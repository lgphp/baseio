package com.generallycloud.nio.balance;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.generallycloud.nio.acceptor.SocketChannelAcceptor;
import com.generallycloud.nio.common.LoggerFactory;
import com.generallycloud.nio.common.LoggerUtil;
import com.generallycloud.nio.component.SocketChannelContext;

public class FrontFacadeAcceptor {

	private AtomicBoolean			started				= new AtomicBoolean(false);
	private SocketChannelAcceptor		acceptor				= null;
	private FrontContext			frontContext;

	public void start(FrontContext frontContext, SocketChannelContext frontBaseContext, SocketChannelContext frontReverseBaseContext)
			throws IOException {

		if (frontContext == null) {
			throw new IllegalArgumentException("null configuration");
		}

		if (!started.compareAndSet(false, true)) {
			return;
		}

		this.frontContext = frontContext;

		this.frontContext.getFrontReverseAcceptor().start(frontReverseBaseContext);

		this.acceptor = new SocketChannelAcceptor(frontBaseContext);

		this.acceptor.bind();

		LoggerUtil.prettyNIOServerLog(LoggerFactory.getLogger(FrontFacadeAcceptor.class),
				"Front Facade Acceptor 启动成功 ...");
	}

	public void stop() {
		if (!started.get()) {
			return;
		}
		this.acceptor.unbind();

		this.frontContext.getFrontReverseAcceptor().stop();
	}

	public FrontContext getFrontContext() {
		return frontContext;
	}

	public SocketChannelAcceptor getAcceptor() {
		return acceptor;
	}

}
