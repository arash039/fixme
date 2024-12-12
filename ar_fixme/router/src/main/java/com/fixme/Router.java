package com.fixme;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Router {
	private final int MAX_CLIENTS = 999999;
	private final int ROUTER_ID = 100000;
	private static BufferedReader bufferReader;
	private static Executor pool;
	private static int brokerIndex;
	private static int marketIndex;
	private static HashMap<String, Socket> brokers;
	private static HashMap<String, Socket> markets;
	private MessageHandler FirstHandler = new FirstHandler();
	private MessageHandler SecondHandler = new SecondHandler();

	public Router() {
		bufferReader = new BufferedReader(new InputStreamReader(System.in));
		pool = Executors.newFixedThreadPool(200); //Executor use for single task
		brokers = new HashMap<>();
		markets = new HashMap<>();
		brokerIndex = ROUTER_ID + 1;
		marketIndex = ROUTER_ID + 1;
		FirstHandler.setNext(SecondHandler);
		SecondHandler.setNext(null);
	}

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

	private void sendToBroker(byte[] message) {
		try {
			FixMessage fixMessage = MessageFactory.createMessage(message);

		} catch (Exception e) {

		}
	}
}
