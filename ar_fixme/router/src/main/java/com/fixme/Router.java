package com.fixme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
//import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class Router {
	private final int MAX_CLIENTS = 999999;
	private final int ROUTER_ID = 100000;
	private static BufferedReader bufferReader;
	private static Executor pool;
	private static int brokerIndex;
	private static int marketIndex;
	private static HashMap<String, ClientAttachment> brokers;
	private static HashMap<String, ClientAttachment> markets;
	private MessageHandler firstHandler = new FirstHandler();
	private MessageHandler secondHandler = new SecondHandler();
	// private static final String DB_URL = "jdbc:mysql://localhost:3306/FixRouter";
	// private static final String DB_USER = "root"; 
	// private static final String DB_PASSWORD = "rootpassword";

	public Router() {
		bufferReader = new BufferedReader(new InputStreamReader(System.in));
		pool = Executors.newFixedThreadPool(200); //Executor use for single task
		brokers = new HashMap<>();
		markets = new HashMap<>();
		brokerIndex = ROUTER_ID + 1;
		marketIndex = ROUTER_ID + 1;
		firstHandler.setNext(secondHandler);
		secondHandler.setNext(null);
	}

	// private void saveTransaction(String senderId, String targetId, String message, String clientOrderId) {
	// 	try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)){
	// 		String sql = "INSERT INTO Transactions (senderId, targetId, message, clientOrderId) VALUES (?, ?, ?, ?)";
	// 		try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
    //             preparedStatement.setString(1, senderId);
    //             preparedStatement.setString(2, targetId);
    //             preparedStatement.setString(3, message);
    //             preparedStatement.setString(4, clientOrderId);
    //             preparedStatement.executeUpdate();
    //             System.out.println("Transaction saved to database.");
    //         }
	// 	} catch (SQLException e) {
	// 		System.err.println("Error saving transaction: " + e.getMessage());
	// 	}
	// }

	abstract class MessageHandler {
		MessageHandler next;

		void messageHandler(byte[] message, String source){};

		public void setNext(MessageHandler _next) {
			this.next = _next;
		}
	}

	class FirstHandler extends MessageHandler {
		@Override
		void messageHandler(byte[] message, String source) {
			if (source.equals("broker")) {
				sendToMarket(message);
			}
			else if (next != null) {
				next.messageHandler(message, source);
			}
		}
	}

	class SecondHandler extends MessageHandler {
		@Override
		void messageHandler(byte[] message, String source) {
			if (source.equals("market")) {
				sendToBroker(message);
			}
			else if (next != null) {
				next.messageHandler(message, source);
			}
		}
	}

	public void acceptMarket() { 
		try (final AsynchronousServerSocketChannel marketChannel = AsynchronousServerSocketChannel.open()) { 
			InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5001); 
			marketChannel.bind(hostAddress); 
			while (true) { 
				marketChannel.accept(marketChannel, new MarketCompletionHandler(markets));
				blocker(); 
			} 
		} catch (IOException e) { 
			System.err.println("Router Error in acceptMarket(): " + e.getMessage()); 
			e.printStackTrace(); 
		} 
	}

	public void acceptBroker() {
		try (final AsynchronousServerSocketChannel brokerChannel = AsynchronousServerSocketChannel.open()) {
			InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5000);
			brokerChannel.bind(hostAddress);
			while (true) {
				brokerChannel.accept(null, new BrokerCompletionHandler(brokers, brokerChannel));
				blocker();
			}
		} catch (IOException e) {
			System.err.println("Router Error in acceptBroker(): " + e.getMessage()); 
			e.printStackTrace(); 
		}
	}

	private void sendToBroker(byte[] message) {
		try {
			FixMessage fixMessage = MessageFactory.createMessage(message);
			Utils.validateChecksum(fixMessage.getStringMessage());
			String senderId = fixMessage.messagMap.get("49");
			String targetId = fixMessage.messagMap.get("56");
			System.out.println("Message sent to broker " + targetId + " from " + senderId);
			pool.execute(new SendToBroker(message, senderId, targetId));
		} catch (FixCheckSumException e) {
			System.err.println(e.getMessage());
		// } catch (FixFormatException e) {
		// 	System.err.println(e.getMessage());
		// } catch (FixMessageException e) {
		// 	System.err.println(e.getMessage());
		}
	}

	private void sendToMarket(byte[] message) {
		try {
			FixMessage fixMessage = MessageFactory.createMessage(message);
			Utils.validateChecksum(fixMessage.getStringMessage());
			String senderId = fixMessage.messagMap.get("49");
			String targetId = fixMessage.messagMap.get("56");
			String clientOrderId = fixMessage.messagMap.get("11");
			//System.out.println(message + " --- " + senderId + " " + targetId + " " + clientOrderId);
			System.out.println("Message sent to market " + targetId + " from " + senderId);
			pool.execute(new SendToMarket(message, senderId, targetId, clientOrderId));
		} catch (FixCheckSumException e) {
			System.err.println("Error! checksum failed" + e.getMessage());
		// } catch (FixMessageException e) {
		// 	System.err.println(e.getMessage());
		// } catch (FixFormatException e) {
		// 	System.err.println(e.getMessage());
		}
	}

	// completed: Called when a read operation completes successfully.
	// result != -1: Checks if the read operation was successful (result is not -1).
	// attachment.buffer.flip(): Prepares the buffer for reading by flipping it from writing mode to reading mode.
	// Extracting Data:
	// int limit = attachment.buffer.limit();: Gets the limit of the buffer (number of bytes to read).
	// byte[] bytes = new byte[limit];: Creates a byte array to hold the data.
	// attachment.buffer.get(bytes, 0, limit);: Reads the data from the buffer into the byte array.
	// Handling Message:
	// dispatchOne.handleMessage(bytes, "market");: Calls a method to handle the message (you might uncomment sendToBroker(bytes) if needed).
	// Clearing Buffer:
	// attachment.buffer.clear();: Clears the buffer for the next read operation.
	// Read Again:
	// attachment.client.read(attachment.buffer, attachment, this);: Initiates another read operation.
	// result == -1: If the read operation indicates the end of the stream:
	// attachment.client.close();: Closes the client connection.
	// attachment.client = null;: Nullifies the client reference to mark it as closed.
	class MarketHandler implements CompletionHandler<Integer, ClientAttachment> {
		@Override
		public void completed(Integer result, ClientAttachment attachment) {
			try {
				if (result != -1) {
					attachment.buffer.flip();
					int limit = attachment.buffer.limit();
					byte[] bytes = new byte[limit];
					attachment.buffer.get(bytes, 0, limit);
					firstHandler.messageHandler(bytes, "market");
					attachment.buffer.clear();
					attachment.client.read(attachment.buffer, attachment, this);
				} else {
					attachment.client.close();
					attachment.client = null;
				}
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}

		@Override
		public void failed(Throwable exc, ClientAttachment attachment) {
			System.err.println("Failed method in MarketHandler called: " + exc.getMessage());
		}
	}
	// two handlers are used for each connection because we need to accept connection also read and write
	class BrokerHandler implements CompletionHandler<Integer, ClientAttachment> {
		@Override
		public void completed(Integer result, ClientAttachment attachment) {
			try {
				if (result != -1) {
					attachment.buffer.flip();
					int limit = attachment.buffer.limit();
					byte[] bytes = new byte[limit];
					attachment.buffer.get(bytes, 0, limit);
					firstHandler.messageHandler(bytes, "broker");
					attachment.buffer.clear();
					attachment.client.read(attachment.buffer, attachment, this); // re-initiates the read process for the next round
				} else {
					attachment.client.close();
					attachment.client = null;
				}
			} catch (IOException e ) {
				System.err.println(e.getMessage());
			}
		}

		@Override
		public void failed(Throwable exc, ClientAttachment attachment) {
			System.err.println("Failed method in MarketHandler called: " + exc.getMessage());
		}
	}

	public class MarketCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {
	
		private Map<String, ClientAttachment> markets;
	
		public MarketCompletionHandler(Map<String, ClientAttachment> markets) {
			this.markets = markets;
		}
	
		@Override
		public void completed(AsynchronousSocketChannel client, AsynchronousServerSocketChannel serverChannel) {
			
			if (client.isOpen()) {
				serverChannel.accept(serverChannel, this);
				registerMarket(client);
			}
		}
	
		private void registerMarket(AsynchronousSocketChannel client) {
			try {
				if (marketIndex < MAX_CLIENTS) {
					String marketID = Integer.toString(marketIndex++);
					String welcomeMessage = "Hello, you are now connected to the router, your ID is " + marketID + "\n";
					System.out.println("Market #" + marketID + " is now connected to the router" );
					client.write(ByteBuffer.wrap(welcomeMessage.getBytes())).get();
					ClientAttachment clientAttachment = new ClientAttachment(client, marketID);
					markets.put(marketID, clientAttachment);
					System.out.println("Waiting for messages from market #" + marketID);
					client.read(clientAttachment.buffer, clientAttachment, new MarketHandler());
				} else {
					System.out.println("Maximum number of clients reached...try again later");
				}
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("Something went wrong while trying to register a market");
				e.printStackTrace();
			}
		}
	
		@Override
		public void failed(Throwable exc, AsynchronousServerSocketChannel serverChannel) {
			System.err.println("Something went wrong while connecting Market to Router");
			exc.printStackTrace();
		}
	}

	public class BrokerCompletionHandler implements CompletionHandler<AsynchronousSocketChannel, Void> {
		private Map<String, ClientAttachment> localBrokers;
		AsynchronousServerSocketChannel brokerChannel;
	
		public BrokerCompletionHandler(Map<String, ClientAttachment> brokers, AsynchronousServerSocketChannel brokerChannel) {
			this.localBrokers = brokers;
			this.brokerChannel = brokerChannel;
		}

		@Override
		public void completed(AsynchronousSocketChannel client, Void attachment) {
			if (client.isOpen()) {
				brokerChannel.accept(null, this);
				registerBroker(client);
			}
		}

		private void registerBroker(AsynchronousSocketChannel client) {
			try {
				if (brokerIndex < MAX_CLIENTS) {
					String brokerId = Integer.toString(brokerIndex++);
					String welcomeMessage = "Hello, you are now connected to the router, your ID is " + brokerId + "\n";
					System.out.println("Broker #" + brokerId + " is now connected to the router" );
					client.write(ByteBuffer.wrap(welcomeMessage.getBytes())).get();
					ClientAttachment clientAttachment = new ClientAttachment(client, brokerId);
					brokers.put(brokerId, clientAttachment);
					System.out.println("Waiting for messages from Broker #" + brokerId);
					client.read(clientAttachment.buffer, clientAttachment, new BrokerHandler());
				} else {
					System.out.println("Maximum number of clients reached...try again later");
				}
			} catch (InterruptedException | ExecutionException e) {
				System.err.println("Something went wrong while trying to register a market");
				e.printStackTrace();
			}
		}

		@Override
		public void failed(Throwable exc, Void attachment) {
			System.err.println("Something went wrong while connecting Broker to Router");
			exc.printStackTrace();
		}
	}

	class SendToMarket implements Runnable {
		private byte[] message;
        private String senderId;
        private String targetId;
        private String clientOrderId;

		SendToMarket(byte[] message, String senderId, String targetId, String clientOrderId) {
			this.message = message;
			this.senderId = senderId;
			this.targetId = targetId;
			this.clientOrderId = clientOrderId;
		}

		@Override
		public void run() {
			try {
				String messageString = new String(message); 
				//saveTransaction(senderId, targetId, messageString, clientOrderId); 
				ClientAttachment clientAttachment = markets.get(targetId);
				//System.out.println(clientAttachment);
				if (clientAttachment != null && clientAttachment.client != null) {
					clientAttachment.client.write(ByteBuffer.wrap(message)).get();
					//clientAttachment.client.write(ByteBuffer.wrap("now get this".getBytes())).get();

				} else {
					FixMessage rejectionMessage = MessageFactory.rejectionMessage(Integer.toString(ROUTER_ID), targetId, clientOrderId, "Mraket #" + targetId + " is not available");
					ClientAttachment snedingClient = brokers.get(senderId);
					if (snedingClient != null) {
						snedingClient.client.write(ByteBuffer.wrap(rejectionMessage.getRawMessage())).get();
					}
				}
			} catch (InterruptedException | ExecutionException e) {
                System.err.println(e.getMessage());
            } catch (FixFormatException e) {
                System.err.println(e.getMessage());
            } catch (FixMessageException e) {
                System.err.println(e.getMessage());
            }
		}
	}

	class SendToBroker implements Runnable {
		private byte[] message;
		private String senderId;
		private String targetId;

		public SendToBroker(byte[] message, String senderId,  String targetId) {
			this.message = message;
			this.senderId = senderId;
			this.targetId = targetId;
		}

		@Override
		public void run() {
			try {
				String messageString = new String(message); 
				//saveTransaction(senderId, targetId, messageString, null);
				ClientAttachment clientAttachment = brokers.get(targetId);
				if (clientAttachment != null && clientAttachment.client != null) {
					clientAttachment.client.write(ByteBuffer.wrap(message)).get(); // get from the Future object This effectively makes the asynchronous operation behave synchronously, as it blocks until the write operation is complete.
				} else {
					printToSender("Broker has disconnected.\n");
				}
			} catch (InterruptedException | ExecutionException e) {
				System.err.println(e.getMessage());
			}
		}

		private void printToSender(String msg) throws ExecutionException, InterruptedException {
            ClientAttachment sendingClient = markets.get(senderId);
            if (sendingClient != null) {
                sendingClient.client.write(ByteBuffer.wrap(msg.getBytes())).get();
            }
        }
	}

	public static void blocker() {
        try {
            bufferReader.readLine();
            blocker();
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

}
