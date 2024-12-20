package com.fixme;

//import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// FIX (Financial Information eXchange) protocol traditionally uses a special
// kind of delimiter, known as the SOH (Start of Header) character, to separate
// fields within a message. This delimiter is represented by the ASCII 
// character 0x01

// Example of FIX message:
// 8=FIX.4.2|9=112|35=D|34=4|49=CLIENT1|56=BROKER1|11=12345|21=1|55=AAPL|54=1|38=100|40=2|44=150.50|10=157|
// 8=FIX.4.2: Begin string and FIX version | 9=112: Body length (number of characters after 9= and before 10=) | 35=D: Message type (New Order Single) | 34=4: Message sequence number | 49=CLIENT1: Sender Comp ID (Client identifier) | 56=BROKER1: Target Comp ID (Broker identifier) | 11=12345: ClOrdID (Client order ID) | 21=1: HandlInst (Handling instruction: 1 = Automated execution) | 55=AAPL: Symbol (Ticker symbol for Apple Inc.) | 54=1: Side (1 = Buy) | 38=100: OrderQty (Order quantity: 100 shares) | 40=2: OrdType (Order type: 2 = Limit order) | 44=150.50: Price (Limit price: $150.50 | 10=157: CheckSum (Checksum of the message).


public class FixMessage {
	private byte[] rawMessage;
	private String stringMessage;
	public HashMap<String, String> messagMap = new HashMap<>();
	private List<String> tags = new ArrayList<>();
	private List<String> values = new ArrayList<>();

	FixMessage(String message) {
		this.rawMessage = Utils.insertSOHDelimiter(message.getBytes());
		this.stringMessage = message;
	}

	FixMessage(byte[] rawMessage) {
		byte[] temp = Arrays.copyOf(rawMessage, rawMessage.length); //length for arrays is a property not a method!
		this.rawMessage = rawMessage;
		this.stringMessage = new String(Utils.insertPrintableDelimiter(temp));
	}

	public byte[] getRawMessage() {
		return rawMessage;
	}

	public String getStringMessage() {
		return stringMessage;
	}

	void parseBytes() {
		int i = 0;
		int len = this.rawMessage.length;
		String _tag;
		String _value;

		while (i < len) {
			_tag = "";
			_value = "";
			while (this.rawMessage[i] != 1) {
				while (this.rawMessage[i] != '=' && this.rawMessage[i] != 1) {
					_tag = _tag + (char)this.rawMessage[i];
					i++;
				}
				if (this.rawMessage[i] == '=') {
					i++;
					while (this.rawMessage[i] != 1) {
						_value = _value + (char)this.rawMessage[i];
						i++;
					}
				}
			}
			tags.add(_tag);
			values.add(_value);
			if (i < len)
				i++;
		}
		//System.out.println("bytes parsed");
	}


	void parseLists() {
		try {
			if ((tags.size() == 0 || values.size() == 0) || tags.size() != values.size())
				throw new FixFormatException("Error!!! One or more tag value pairs are missing.");
			if (!tags.get(0).equals("49"))
				throw new FixFormatException("Error! FIX message must start with the internal sender ID.");
			for (int i = 0; i < tags.size(); i++) {
				//System.out.println(tags.get(i) +"-----" + values.get(i));
				if (tags.get(i).isEmpty() || values.get(i).isEmpty()) {
					System.out.println(tags.get(i) + " --- " + values.get(i));
					throw new FixFormatException("Error! One or more tag value pairs are missing.");
				}
				if (!messagMap.containsKey(tags.get(i)))
					messagMap.put(tags.get(i), values.get(i));
				else
					throw new FixFormatException("Error! Message contains duplicated tags.");
			} 
		} catch (FixFormatException e) {
				System.err.println(e);
		}
		//System.out.println("list parsed");
	}

	void checkMessageMap() {
		try {
			if (messagMap.get("56") == null) {
				System.out.println("ERROR");
				throw new FixMessageException("Error! FIX message must contain an internal target ID.");
			}
		} catch (Exception e) {
			System.err.println(e);
		}
		//System.out.println("map checked");
	}

	public static String makeChecksum(byte[] message) {
		int bytesSum = 0;

		for (int i = 0; i < message.length; i++)
			bytesSum += message[i];
		String checksum = Integer.toString(bytesSum % 256);
		//System.out.println("calculated checksum is: " + checksum);
		switch (checksum.length()) { // for consitency make checksum 3 digits
			case 1:
				checksum = "00" + checksum;
				break;
			case 2:
				checksum = "0" + checksum;
				break;
			default:
				break;
		}
		return checksum;
	}

	void appendChecksum() {
		this.stringMessage = this.stringMessage + "10" + "=" + makeChecksum(this.rawMessage) + '|';
		byte[] temp = new byte[rawMessage.length + 7];
		byte[] tag = ("10" + "=").getBytes();
		byte[] checksumBytes = makeChecksum(this.rawMessage).getBytes();

		System.arraycopy(rawMessage, 0, temp, 0, rawMessage.length);
		System.arraycopy(tag, 0, temp, rawMessage.length, 3);
		System.arraycopy(checksumBytes, 0, temp, rawMessage.length + 3, 3);
		temp[temp.length - 1] = 1;

		this.rawMessage = temp;
		//System.out.println("checksum added: " + stringMessage);
	}

}
