package com.fixme;

import java.util.Random;

public class Utils {
	private static final Random rand = new Random();

	public static byte[] insertSOHDelimiter(byte[] message) {
		for (int i = 0; i < message.length; i++) {
			if (message[i] == '|') {
				message[i] = 1;
			}
		}
		return message;
	}

	public static byte[] insertPrintableDelimiter(byte[] message) {
		for (int i = 0; i < message.length; i++) {
			if (message[i] == 1) {
				message[i] = '|';
			}
		}
		return message;
	}

	public static void validateChecksum(String message) throws FixCheckSumException{
		int checksumIndex = message.lastIndexOf("|10=");
		if (checksumIndex == -1) {
			throw new FixCheckSumException(FixCheckSumException.checkSumMissing);
		}
		String checksumStr = message.substring(checksumIndex + 4, checksumIndex + 7);
		int checksumValue = Integer.parseInt(checksumStr);
		String strippedMessage = message.substring(0, checksumIndex + 1);
		byte[] strippedBytes = insertSOHDelimiter(strippedMessage.getBytes());
		//System.out.println(checksumValue + " ----- " + strippedMessage + " ---- ");
		strippedMessage = FixMessage.makeChecksum(strippedBytes);
		//System.out.println(strippedMessage);
		if (checksumValue == Integer.parseInt(strippedMessage))
			return;
		else
			throw new FixCheckSumException(FixCheckSumException.checkSumIncorrect);
	}

	public static String idGenerator() {
		int randNumber = rand.nextInt(100000);
		long timeStamp = System.currentTimeMillis();

		return Integer.toHexString(randNumber) + "_" + Long.toString(timeStamp);
	}
}
