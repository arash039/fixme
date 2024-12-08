package router.src.main.java;

// MessageHandler.java
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class MessageHandler implements Runnable {
    private final Socket socket;
    private final ConcurrentHashMap<Integer, Socket> brokerSockets;
    private final ConcurrentHashMap<Integer, Socket> marketSockets;
    private final boolean isBroker;

    public MessageHandler(Socket socket, ConcurrentHashMap<Integer, Socket> brokerSockets, ConcurrentHashMap<Integer, Socket> marketSockets, boolean isBroker) {
        this.socket = socket;
        this.brokerSockets = brokerSockets;
        this.marketSockets = marketSockets;
        this.isBroker = isBroker;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received: " + message);
                if (validateMessage(message)) {
                    routeMessage(message);
                } else {
                    System.out.println("Invalid message: " + message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean validateMessage(String message) {
        // Implement checksum validation logic here
        return true; // Placeholder for actual validation
    }

    private void routeMessage(String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 5) {
            System.out.println("Invalid message format: " + message);
            return;
        }

        int destinationId = Integer.parseInt(parts[4]); // Assuming the destination ID is in the 5th position
        Socket destinationSocket = isBroker ? marketSockets.get(destinationId) : brokerSockets.get(destinationId);

        if (destinationSocket != null) {
            try {
                PrintWriter out = new PrintWriter(destinationSocket.getOutputStream(), true);
                out.println(message);
                System.out.println("Routed message to ID: " + destinationId);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Destination ID not found: " + destinationId);
        }
    }
}
