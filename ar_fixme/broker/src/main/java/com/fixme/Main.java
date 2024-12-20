package com.fixme;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        Broker broker = new Broker();
		try {
			broker.start();
			broker.readId();
			broker.readWriteHandler();
		} catch (InterruptedException | ExecutionException | IOException e ){
			System.out.println("There was an error connecting to the server, please ensure that it is online: " + e. getMessage());
		}
    }
}