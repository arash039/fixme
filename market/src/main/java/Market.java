package market.src.main.java;

// Market.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Market {
    private static final String ROUTER_HOST = "localhost";
    private static final int ROUTER_PORT = 5001;
    private static int marketId;
    private static final Map<String, Integer> instruments = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        // Initialize the market with some instruments
        instruments.put("Instrument1", 100);
        instruments.put("Instrument2", 50);

        Socket socket = new Socket(ROUTER_HOST, ROUTER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        marketId = Integer.parseInt(in.readLine().split(":")[1]);
        System.out.println("Market ID: " + marketId + " " + instruments.get("Instrument1"));

        // Handle incoming orders
        handleOrders(in, out);
    }

    private static void handleOrders(BufferedReader in, PrintWriter out) {
        try {
            String order;
            while ((order = in.readLine()) != null) {
                System.out.println("Received order: " + order);
                String[] parts = order.split("\\|");
                int brokerId = Integer.parseInt(parts[0]);
                String type = parts[1];
                String instrument = parts[2].trim(); // Trim any leading or trailing spaces
                int quantity = Integer.parseInt(parts[3]);
                String market = parts[4];
                int price = Integer.parseInt(parts[5]);

                if (marketId == Integer.parseInt(market)) {
                    if (type.equals("BUY")) {
                        executeBuyOrder(out, brokerId, instrument, quantity);
                    } else if (type.equals("SELL")) {
                        executeSellOrder(out, brokerId, instrument, quantity);
                    }
                } else {
                    out.println(brokerId + "|Rejected|Checksum");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void executeBuyOrder(PrintWriter out, int brokerId, String instrument, int quantity) {
        System.out.println("Executing buy order for " + quantity + " of " + instrument + "from " + instruments.get(instrument) + " existing" + "     " + instruments.get("instrument1"));
        if (instruments.containsKey(instrument) && instruments.get(instrument) >= quantity) {
            instruments.put(instrument, instruments.get(instrument) - quantity);
            out.println(brokerId + "|Executed|Checksum");
            System.out.println("Executed buy order for " + quantity + " of " + instrument);
        } else {
            out.println(brokerId + "|Rejected|Checksum");
            System.out.println("Rejected buy order for " + quantity + " of " + instrument);
        }
    }

    private static void executeSellOrder(PrintWriter out, int brokerId, String instrument, int quantity) {
        System.out.println("Executing sell order for " + quantity + " of " + instrument);
        instruments.put(instrument, instruments.getOrDefault(instrument, 0) + quantity);
        out.println(brokerId + "|Executed|Checksum");
        System.out.println("Executed sell order for " + quantity + " of " + instrument);
    }
}




