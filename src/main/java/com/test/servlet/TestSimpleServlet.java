package com.test.servlet;

import com.gifisan.nio.common.StringUtil;
import com.gifisan.nio.component.future.ServerReadFuture;
import com.gifisan.nio.server.service.NIOServlet;
import com.gifisan.nio.server.session.IOSession;

public class TestSimpleServlet extends NIOServlet{
	
	private TestSimple1 simple1 = new TestSimple1();
	
//	private AtomicInteger size = new AtomicInteger();

	public void accept(IOSession session,ServerReadFuture future) throws Exception {

		String test = future.getText();

		if (StringUtil.isNullOrBlank(test)) {
			test = "test";
		}
		future.write(simple1.dynamic());
		future.write(test);
		future.write("$");
		session.flush(future);
		
//		System.out.println("=============================="+size.incrementAndGet());
	}

}