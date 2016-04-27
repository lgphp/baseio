package test;

import java.io.IOException;

import com.gifisan.nio.client.ClientConnector;
import com.gifisan.nio.client.ClientSession;
import com.gifisan.nio.common.CloseUtil;
import com.gifisan.nio.component.future.ReadFuture;

public class TestGetPhoneNO {
	
	
	public static void main(String[] args) throws IOException {


		String serviceKey = "TestGetPhoneNOServlet";
		
		ClientConnector connector = ClientUtil.getClientConnector();
		connector.connect();
		ClientSession session = connector.getClientSession();
		
		ReadFuture future = session.request(serviceKey, null);
		System.out.println(future.getText());
		
		CloseUtil.close(connector);
		
	}
}