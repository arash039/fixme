package router.src.main.java;

// Router.java
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Router {
    private static final int BROKER_PORT = 5000;
    private static final int MARKET_PORT = 5001;
    private static final ConcurrentHashMap<Integer, Socket> brokerSockets = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Socket> marketSockets = new ConcurrentHashMap<>();
    private static int brokerIdCounter = 100000;
    private static int marketIdCounter = 200000;

    public static void main(String[] args) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        ServerSocket brokerServerSocket = new ServerSocket(BROKER_PORT);
        ServerSocket marketServerSocket = new ServerSocket(MARKET_PORT);

        executor.submit(() -> handleConnections(brokerServerSocket, brokerSockets, true));
        executor.submit(() -> handleConnections(marketServerSocket, marketSockets, false));
    }

    private static void handleConnections(ServerSocket serverSocket, ConcurrentHashMap<Integer, Socket> sockets, boolean isBroker) {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                int id = isBroker ? brokerIdCounter++ : marketIdCounter++;
                sockets.put(id, socket);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("ID:" + id);
                new Thread(new MessageHandler(socket, brokerSockets, marketSockets, isBroker)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


