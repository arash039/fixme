import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.fixme.Router;
import com.fixme.ClientAttachment;



public class RouterTest {

	private Router router;
	private AsynchronousServerSocketChannel mockBrokerChannel;
	private AsynchronousServerSocketChannel mockMarketChannel;
	private AsynchronousSocketChannel mockClient;
	private ClientAttachment mockClientAttachment;

	@Before
	public void setUp() throws IOException {
		router = new Router();
		mockBrokerChannel = mock(AsynchronousServerSocketChannel.class);
		mockMarketChannel = mock(AsynchronousServerSocketChannel.class);
		mockClient = mock(AsynchronousSocketChannel.class);
		mockClientAttachment = mock(ClientAttachment.class);
	}

	@Test
	public void testAcceptBroker() throws IOException {
		when(mockBrokerChannel.bind(any(InetSocketAddress.class))).thenReturn(mockBrokerChannel);
		doNothing().when(mockBrokerChannel).accept(any(), any());

		router.acceptBroker();

		verify(mockBrokerChannel).bind(any(InetSocketAddress.class));
		verify(mockBrokerChannel).accept(any(), any());
	}

	@Test
	public void testAcceptMarket() throws IOException {
		when(mockMarketChannel.bind(any(InetSocketAddress.class))).thenReturn(mockMarketChannel);
		doNothing().when(mockMarketChannel).accept(any(), any());

		router.acceptMarket();

		verify(mockMarketChannel).bind(any(InetSocketAddress.class));
		verify(mockMarketChannel).accept(any(), any());
	}

	@Test
	public void testSendToBroker() throws Exception {
		byte[] message = "test message".getBytes();
		String senderId = "100001";
		String targetId = "100002";
		ClientAttachment clientAttachment = new ClientAttachment(mockClient, targetId);
		Router.brokers.put(targetId, clientAttachment);

		router.sendToBroker(message);

		verify(mockClient).write(ByteBuffer.wrap(message));
	}

	@Test
	public void testSendToMarket() throws Exception {
		byte[] message = "test message".getBytes();
		String senderId = "100001";
		String targetId = "100002";
		String clientOrderId = "12345";
		ClientAttachment clientAttachment = new ClientAttachment(mockClient, targetId);
		Router.markets.put(targetId, clientAttachment);

		router.sendToMarket(message);

		verify(mockClient).write(ByteBuffer.wrap(message));
	}
}
