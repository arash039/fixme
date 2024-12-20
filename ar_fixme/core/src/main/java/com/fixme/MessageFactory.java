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

	public static FixMessage formMessage(String internalSenderID, String internalTargetID, String side, String symbol, String quantity, String price) throws FixMessageException, FixFormatException {
		//valQuantityInput(quantity);
		//valPriceInput(price);
		String finalBuyMsg = "49" + "=" + internalSenderID + '|' + "56" + "=" + internalTargetID + '|' + "35" + "=" + "D" + '|' + "54" + "=" + side + '|' + "55" + "=" + symbol + '|' + "44" + "=" + price + '|' + "38" + "=" + quantity + '|' + "11" + "=" + Utils.idGenerator() + '|';
		//System.out.println("MAKING THE MESSAGE" + finalBuyMsg);
		FixMessage fixMessage = new FixMessage(finalBuyMsg);

		//fixMessage.checkFixFormat();
		fixMessage.appendChecksum();
		fixMessage.parseBytes();
		fixMessage.parseLists();
		fixMessage.checkMessageMap();

		return fixMessage;
  	}
	
	public static FixMessage rejectionMessage(String interlaSenderId, String internalTargetId, String clientOrderId, String rejectionReason) throws FixMessageException, FixFormatException{
		String message = "49" + "=" + interlaSenderId + '|' + "56" + "=" + internalTargetId + '|' + "35" + "=" + "8" + '|' + "11" + "=" + clientOrderId + '|' + "150" + "=" + "2" + '|' + "58" + "=" + rejectionReason + '|';
		FixMessage fixMessage = new FixMessage(message);
		fixMessage.appendChecksum();
		fixMessage.parseBytes();
		fixMessage.parseLists();
		fixMessage.checkMessageMap();
		return fixMessage;
	}

	public static FixMessage successMessage(String interlaSenderId, String internalTargetId, String clientOrderId) throws FixMessageException, FixFormatException{
		String message = "49" + "=" + interlaSenderId + '|' + "56" + "=" + internalTargetId + '|' + "35" + "=" + "8" + '|' + "11" + "=" + clientOrderId + '|' + "150"  + "=" + "7" + '|';
		
		FixMessage fixMessage = new FixMessage(message);
		fixMessage.appendChecksum();
		fixMessage.parseBytes();
		fixMessage.parseLists();
		fixMessage.checkMessageMap();
		return fixMessage;
	}

}
