package com.fixme;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Broker {
	private AsynchronousSocketChannel client;
	private volatile boolean shouldExit = false;
	private Future<Void> future;
	private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	private static Console console = System.console();
	private static String brokerId = null;
	private static HashMap<String, Object> attachment = new HashMap<>();
	private static boolean runInputReader = true;
	private static Pattern idPattern = Pattern.compile("^Hello, you are now connected to the router, your ID is (\\d+)");

	String RESET = "\u001B[0m";
	String RED = "\u001B[31m";
	String GREEN = "\u001B[32m";
	String YELLOW = "\u001B[33m";
	String BLUE = "\u001B[34m";

	void start() {
		try{
			client = AsynchronousSocketChannel.open();
			InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5000);
			future = client.connect(hostAddress);
			future.get();
		} catch (ExecutionException | InterruptedException | IOException e) {
			e.printStackTrace();
		}
	}

	void readId() throws ExecutionException, InterruptedException {
		String routerMessgae;
		ByteBuffer buffer = ByteBuffer.allocate(64);
		int bytesRead = client.read(buffer).get();
		if (bytesRead == -1) {
			System.out.println("Router disconnected");
			//stop();
		}
		buffer.flip();
		routerMessgae = new String(buffer.array());
		//System.out.println(routerMessgae);
		routerMessgae = routerMessgae.trim();
		Matcher idMatcher = idPattern.matcher(routerMessgae);
		if (idMatcher.find())
			brokerId = idMatcher.group(1);
		if (brokerId == null) {
			System.out.println("Error connecting to router");
			stop();
		} else {
			System.out.println("Hi. You are connected to the router. Broker Id is: " + brokerId );
			System.out.println("You can also type help, or exit.");
		}
	}

	void readWriteHandler() throws IOException, ExecutionException, InterruptedException {
		ByteBuffer buffer = ByteBuffer.allocate(512);
		ReadAttachment readAttachment = new ReadAttachment(buffer);
		client.read(readAttachment.buffer, readAttachment, new ReadHandler());
				
		while (!shouldExit) {
			try {
				TimeUnit.MILLISECONDS.sleep(100);
				//System.out.println("Enter the message:");
				//String line = reader.readLine();
				String line = console.readLine("Enter the message: ");
				if (line == null || line.equalsIgnoreCase("exit")) {
					System.out.println("Broker has disconnected.");
					stop();
					break;
				} else if (line.equalsIgnoreCase("help")) {
					System.out.println(YELLOW + "Sample message format: orderSide: Buy, targetId: 100001, symbol: inst1, quantity: 20, price: 100.00" + RESET);
					continue;
				}
				brokerInputReader(line);
				console.flush();
			} catch (FixFormatException | FixMessageException e) {
				System.out.println("There was an error taking input from the broker: " + e.getMessage());
				System.out.println("Please try again or type 'exit' to quit.");
			}
		}
	}
	

	private void brokerInputReader(String _message) throws ExecutionException, InterruptedException, FixFormatException, FixMessageException {
		String message = _message;
		String orderSide = "";
		String targetId = "";
		String symbol = "";
		String quantity = "";
		String price = "";
		FixMessage fixMessage = null;
		
		String regex = "orderSide: (\\w+), targetId: (\\d+), symbol: (\\w+), quantity: (\\d+), price: (\\d+\\.\\d+)";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(message);

		if (matcher.find()) {
			orderSide = matcher.group(1);
			targetId = matcher.group(2);
			symbol = matcher.group(3);
			quantity = matcher.group(4);
			price = matcher.group(5);

			if (orderSide.trim().toLowerCase().equals("buy")) {
				orderSide = "1";
			}
			else if ((orderSide.trim().toLowerCase().equals("sell"))) {
				orderSide = "2";
			}
			else {
				orderSide = "";
			}
			fixMessage = MessageFactory.formMessage(brokerId, targetId, orderSide, symbol, quantity, price);
			System.out.println("FIX message created:\n" + fixMessage.getStringMessage());
			client.write(ByteBuffer.wrap(fixMessage.getRawMessage())).get();
		} else {
			System.out.println(RED + "Message format is incorrect." + RESET);
		}
	}


	class ReadAttachment {
        public ByteBuffer buffer;

        ReadAttachment(ByteBuffer buffer) {
            this.buffer = buffer;
        }
    }

	class ReadHandler implements CompletionHandler<Integer, ReadAttachment> {
		@Override
		public void completed(Integer result, ReadAttachment attachment) {
			if (result != -1) {
				attachment.buffer.flip();
				int limit = attachment.buffer.limit();
				byte[] bytes = new byte[limit];
				attachment.buffer.get(bytes, 0, limit);
				printMessage(bytes);
				attachment.buffer.clear();
				client.read(attachment.buffer, attachment, this);
			} else {
				System.out.println("Server has disconnected.");
				stop();
			}
		}

		private void printMessage(byte[] message) {
			byte[] temp = Arrays.copyOf(message, message.length);
			String rawMessage = new String(Utils.insertPrintableDelimiter(temp));
			String marketId;
			String clientOrderId;
			String rejectionReason;
			String sender = "market";

			//System.out.println("Raw message from router: " + rawMessage);
			//try {
				FixMessage fixMessage = MessageFactory.createMessage(message);
				marketId = fixMessage.messagMap.get("49");
				clientOrderId = fixMessage.messagMap.get("11");
				if (fixMessage.messagMap.get("150").equals("7")) {
                    System.out.println(GREEN + "Order #" + clientOrderId + " from market #" + marketId + " has been successfully processed.\n" + RESET);
                }
                else if (fixMessage.messagMap.get("150").equals("2")) {
                    if (marketId.equals("100000")) {
                        sender = "router";
                    }
                    System.out.println(RED + "Order #" + clientOrderId + " from " + sender + " #" + marketId + " has been rejected." + RESET);
                    rejectionReason = fixMessage.messagMap.get("58");
                    if (rejectionReason != null) {
                        System.out.println(RED + "Rejection reason is: " + rejectionReason + "\n" + RESET);
                    }
                }
            // } catch (FixFormatException e) {
            //     System.out.println("Error creating fix message from server: " + e.getMessage());
            // } catch (FixMessageException e) {
            //     System.out.println("Error creating fix message from server: " + e.getMessage());
            // }
        }

        @Override
        public void failed(Throwable exc, ReadAttachment attachment) {
            System.out.println(RED + "There was an error reading from the server." + RESET);
        }
	}

	private void stop() {
		shouldExit = true;
		try {
			client.close();
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
}
