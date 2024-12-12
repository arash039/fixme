package com.fixme;

public abstract class MessageFactory {
	public static FixMessage createMessage(String message) {
		FixMessage fixMessage = new FixMessage(message);

		//fixMessage.checkFormat();
		fixMessage.parseBytes();
		fixMessage.parseLists();
		fixMessage.checkMessageMap();

		return fixMessage;
	}
	
	public static FixMessage createMessage(byte[] message) {
		FixMessage fixMessage = new FixMessage(message);

		// fixMessage.checkFormat();
		fixMessage.parseBytes();
		fixMessage.parseLists();
		fixMessage.checkMessageMap();

		return fixMessage;
	}

	// public static FixMessage creatMessage(String senderId, String targetId, String side, String symbol, String quantity, String price) {

	// }
}
