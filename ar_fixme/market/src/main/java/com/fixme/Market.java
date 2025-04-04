package com.fixme;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Market {
	private AsynchronousSocketChannel client;
	private Future<Void> future;
	private static Pattern idPattern = Pattern.compile("^Hello, you are now connected to the router, your ID is (\\d+)");
	private static String marketId = null;
	private static String[] instruments = {"inst1", "inst2", "inst3", "inst4"};
	private double basePrice = 100.00;
	private HashMap<String, Integer> Stock;

	String RESET = "\u001B[0m";
	String RED = "\u001B[31m";
	String GREEN = "\u001B[32m";
	String YELLOW = "\u001B[33m";
	String BLUE = "\u001B[34m";

	Market(double basePrices) {
		Stock = new HashMap<String, Integer>();
		int stockSelector;
		this.basePrice = basePrices;

		for (int i = 0; i < 4; i++) {
			stockSelector = ThreadLocalRandom.current().nextInt(0, 4);
			if (!Stock.containsKey(Market.instruments[stockSelector]))
				Stock.put(Market.instruments[stockSelector], 50);
			else
				i--;
		}
		//System.out.println("available stock is: " + Stock);
		printStockPrices();
	}

	void start() throws IOException, ExecutionException, InterruptedException {
		client = AsynchronousSocketChannel.open();
		InetSocketAddress hostAddress = new InetSocketAddress("localhost", 5001);
		future = client.connect(hostAddress);
		future.get();
	}

	void redId() throws IOException, ExecutionException, InterruptedException {
		String routerMessage;
		ByteBuffer buffer = ByteBuffer.allocate(128);
		int bytesRead = client.read(buffer).get();
		if (bytesRead == -1) {
			System.out.println("Router has disconnected.");
			this.client.close();
			System.exit(0);
		}
		buffer.flip();
		routerMessage = new String(buffer.array());
		routerMessage = routerMessage.trim();
		Matcher matcher = idPattern.matcher(routerMessage);
		if (matcher.find())
			Market.marketId = matcher.group(1);
		if (marketId == null) {
			System.out.println("Not able to connect to router.");
			this.client.close();
		} else {
			System.out.println("Market is connected to router ID is: " + Market.marketId);
		}
	}

	void readHandler() throws IOException, ExecutionException, InterruptedException {
		String senderId;
		String clientOrderId;
		int limit;
		byte[] bytes;
		ByteBuffer buffer = ByteBuffer.allocate(512);
		int bytesRead = client.read(buffer).get();

		if (bytesRead == -1) {
			System.out.println("Router has disconnected");
			this.client.close();
			System.exit(0);
		}

		buffer.flip();
		limit = buffer.limit();
		bytes = new byte[limit];
		buffer.get(bytes, 0, limit);
		buffer.clear();

		try {
			FixMessage fixMessage = MessageFactory.createMessage(bytes);
			System.out.println("Message recieved from the router: " + fixMessage.getStringMessage() + " and will be processed.");
			senderId = fixMessage.messagMap.get("49");
			clientOrderId = fixMessage.messagMap.get("11");
			int resCode = 0;
			//System.out.println("check the reading: " + senderId + "  " + clientOrderId);
			if (fixMessage.messagMap.get("54").equals("1")) {
				String _instrument = fixMessage.messagMap.get("55");
				int _quantity = Integer.parseInt(fixMessage.messagMap.get("38"));
				double _price = Double.parseDouble(fixMessage.messagMap.get("44"));

				resCode = marketOperations(this.Stock, _instrument, _quantity, _price, "buy");

				if (resCode == 420) {
					FixMessage responseFixMessage = MessageFactory.successMessage(Market.marketId, senderId, clientOrderId);
					System.out.println(GREEN + "Order successfully processed. The reply message is: \n" + responseFixMessage.getStringMessage() + RESET);
					client.write(ByteBuffer.wrap(responseFixMessage.getRawMessage())).get();
					return;
				} else {
					rejectionHandler(resCode, senderId, clientOrderId);
				}
			} else if (fixMessage.messagMap.get("54").equals("2")) {
				String _instrument = fixMessage.messagMap.get("55");
				int _quantity = Integer.parseInt(fixMessage.messagMap.get("38"));
				double _price = Double.parseDouble(fixMessage.messagMap.get("44"));

				resCode = marketOperations(this.Stock, _instrument, _quantity, _price, "sell");

				if (resCode == 420) {
					FixMessage responseFixMessage = MessageFactory.successMessage(Market.marketId, senderId, clientOrderId);
					System.out.println(GREEN + "Order successfully processed. The reply message is: \n" + responseFixMessage.getStringMessage() + RESET);
					client.write(ByteBuffer.wrap(responseFixMessage.getRawMessage())).get();
					return;
				} else {
					rejectionHandler(resCode, senderId, clientOrderId);
				}
			}
		} catch (FixFormatException | FixMessageException e) {
			System.out.println("There was an error building the FIX message: " + e.getMessage());
		}
	}

	int marketOperations(HashMap<String, Integer> stock, String instrument, int quantity, double price, String buyOrSell) {
		//System.out.println("in market operations   ->" + buyOrSell);
		double maxPrice = basePrice * 6.00;
		double minPrice = basePrice / 4.00;
		double marketPrice;
		//System.out.println("min price is: " + minPrice + " max price is:  " + maxPrice);
		if (stock.containsKey(instrument)) {
			if (buyOrSell.toLowerCase() == "buy") {
				if (stock.get(instrument) <= 0)
					return 504;
				marketPrice = this.basePrice * (50.00 / stock.get(instrument));
				//marketPrice = basePrice;
				if (marketPrice < minPrice)
					marketPrice = minPrice;
				if (stock.get(instrument) < quantity)
					return 504;
				if (price < 0.9 * marketPrice || price < minPrice)
					return 601;
				else if (price > 1.2 * marketPrice)
					return 602;
				else {
					stock.put(instrument, (stock.get(instrument) - quantity));
					this.Stock = stock;
					return 420;
				}
			} else if (buyOrSell.toLowerCase() == "sell") {
				if (stock.get(instrument) <= 0) {
					marketPrice = maxPrice;
				} else {
					marketPrice = basePrice * (50.0 / stock.get(instrument));
				}
				//System.out.println(BLUE + marketPrice + " " + stock.get(instrument) + RESET);
				//if (marketPrice < minPrice)
				// if (price < minPrice)
				// 	return 505;
				// if (quantity + stock.get(instrument) > 200)
				// 	return 506;
				// if (price < 0.8 * marketPrice || price < minPrice)
				// 	return 601;
				// else if (price > 1.1 * marketPrice || price > maxPrice)
				// 	return 602;
				if (price > marketPrice) {
					System.out.println(YELLOW + "offered price " + price + " is higher than " + marketPrice + RESET);
					return 506;
				} else {
					stock.put(instrument, (stock.get(instrument) + quantity));
					this.Stock = stock;
					return 420;
				}
			}
		} else {
			return 404; //instrument not found
		}
		return -1;
	}

	void rejectionHandler(int resCode, String senderId, String clientOrderId) throws ExecutionException, InterruptedException {
		try {
			if (resCode == 404) {
				FixMessage rejectMessage = MessageFactory.rejectionMessage(Market.marketId, senderId, clientOrderId, "Market #" + Market.marketId + " doesn't have this instrument");
				System.out.println(RED + "Order rejected. The reply message is: \n" + rejectMessage.getStringMessage() + RESET);
				client.write(ByteBuffer.wrap(rejectMessage.getRawMessage())).get();
				return;
			} else if (resCode == 504) {
				FixMessage rejectMessage = MessageFactory.rejectionMessage(Market.marketId, senderId, clientOrderId, "Market #" + Market.marketId + " doesn't have enough amount from this instrument");
				System.out.println(RED + "Order rejected. The reply message is: \n" + rejectMessage.getStringMessage() + RESET);
				client.write(ByteBuffer.wrap(rejectMessage.getRawMessage())).get();
				return;
			} else if (resCode == 505) {
				FixMessage rejectMessage = MessageFactory.rejectionMessage(Market.marketId, senderId, clientOrderId, "Market #" + Market.marketId + " doesn't accept this instrument anymore");
				System.out.println(RED + "Order rejected. The reply message is: \n" + rejectMessage.getStringMessage() + RESET);
				client.write(ByteBuffer.wrap(rejectMessage.getRawMessage())).get();
				return;
			} else if (resCode == 506) {
				FixMessage rejectMessage = MessageFactory.rejectionMessage(Market.marketId, senderId, clientOrderId, "Market #" + Market.marketId + " will not buy this order");
				System.out.println(RED + "Order rejected. The reply message is: \n" + rejectMessage.getStringMessage() + RESET);
				client.write(ByteBuffer.wrap(rejectMessage.getRawMessage())).get();
				return;
			} else if (resCode == -1) {
				FixMessage rejectMessage = MessageFactory.rejectionMessage(Market.marketId, senderId, clientOrderId, "Error in performing request to Market #" + Market.marketId);
				System.out.println(RED + "Order rejected. The reply message is: \n" + rejectMessage.getStringMessage() + RESET);
				client.write(ByteBuffer.wrap(rejectMessage.getRawMessage())).get();
				return;
			} else
				priceReject(resCode, senderId, clientOrderId);
		} catch (FixFormatException | FixMessageException e) {
			System.out.println("There was an error building the FIX message: " + e.getMessage());
		}
	}

	public void printStockPrices() {
		System.out.println(BLUE + "Available Stock: " + RESET);
		for (Map.Entry<String, Integer> entry : Stock.entrySet()) {
			String stockName = entry.getKey();
			int stockValue = entry.getValue();
			double calculatedPrice;
			if (stockValue != 0)
				calculatedPrice = basePrice * (50.00 / stockValue);
			else
				calculatedPrice = basePrice * 6.00;
			System.out.println(BLUE + stockName + " - Available Amount: " + stockValue + " - Calculated Price: " + calculatedPrice + RESET);
		}
	}

	void priceReject(int resCode, String senderId, String clientOrderId) throws ExecutionException, InterruptedException {
		try {
			if (resCode == 601) {
				FixMessage rejectMessage = MessageFactory.rejectionMessage(Market.marketId, senderId, clientOrderId, "Offered price is under the minimum price of Market #" + Market.marketId);
				client.write(ByteBuffer.wrap(rejectMessage.getRawMessage())).get();
				return;
			} else if (resCode == 602) {
				FixMessage rejectMessage = MessageFactory.rejectionMessage(Market.marketId, senderId, clientOrderId, "Offered price is over the maximum price of Market #" + Market.marketId);
				client.write(ByteBuffer.wrap(rejectMessage.getRawMessage())).get();
				return;
			}
		} catch (FixFormatException | FixMessageException e) {
			System.out.println("There was an error building the FIX message: " + e.getMessage());
		}
	}
}
