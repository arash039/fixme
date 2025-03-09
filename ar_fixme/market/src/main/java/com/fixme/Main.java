package com.fixme;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) {
       Market market = new Market(100);

	   try {
		market.start();
		market.redId();
		while (true) {
			System.out.println("Waiting for messages ... ");
			market.readHandler();
			market.printStockPrices();
		}
	   } catch (InterruptedException e) {
		System.out.println("Something went wrong communicating with the server: " + e.getMessage());
	   } catch (ExecutionException | IOException e) {
            System.out.println("Unable to establish connection with router: " + e.getMessage());
		}
    }
}
