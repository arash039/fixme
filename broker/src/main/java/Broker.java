package broker.src.main.java;

// Broker.java
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Broker {
    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 5000;
    private static int brokerId;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket(ROUTER_HOST, ROUTER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        brokerId = Integer.parseInt(in.readLine().split(":")[1]);
        System.out.println("Broker ID: " + brokerId);

        // Start a thread to handle responses from the market
        new Thread(() -> handleResponses(in)).start();

        // Keep the broker open to send messages
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Enter order type (BUY/SELL), instrument, quantity, market, and price:");
            String type = scanner.next();
            String instrument = scanner.next();
            int quantity = scanner.nextInt();
            String market = scanner.next();
            int price = scanner.nextInt();

            sendOrder(out, type, instrument, quantity, market, price);
        }
    }

    private static void sendOrder(PrintWriter out, String type, String instrument, int quantity, String market, int price) {
        String message = brokerId + "|" + type + "|" + instrument + "|" + quantity + "|" + market + "|" + price + "|Checksum";
        out.println(message);
        System.out.println("Sent: " + message);
    }

    private static void handleResponses(BufferedReader in) {
        try {
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("Received: " + response);
                // Process the response
                if (response.contains("Executed")) {
                    System.out.println("Order executed successfully.");
                } else if (response.contains("Rejected")) {
                    System.out.println("Order was rejected.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
