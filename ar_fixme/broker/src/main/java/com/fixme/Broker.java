package com.fixme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class Broker {
	private AsynchronousSocketChannel client;
	private Future<Void> future;
	private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	private static String brokerId = null;

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


}
