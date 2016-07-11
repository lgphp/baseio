package com.test.servlet;

import com.gifisan.nio.common.StringUtil;
import com.gifisan.nio.component.Session;
import com.gifisan.nio.component.future.nio.NIOReadFuture;
import com.gifisan.nio.extend.service.FutureAcceptorService;

public class TestSessionDisconnectServlet extends FutureAcceptorService{
	
	public static final String SERVICE_NAME = TestSessionDisconnectServlet.class.getSimpleName();
	
	private TestSimple1 simple1 = new TestSimple1();
	
//	private AtomicInteger size = new AtomicInteger();

	protected void doAccept(Session session, NIOReadFuture future) throws Exception {

		String test = future.getText();

		if (StringUtil.isNullOrBlank(test)) {
			test = "test";
		}
		future.write(simple1.dynamic());
		future.write(test);
		future.write("$");
		session.flush(future);
		
		session.disconnect();
		
	}

}
