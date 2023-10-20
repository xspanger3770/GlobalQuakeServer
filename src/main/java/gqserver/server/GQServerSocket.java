package gqserver.server;

import gqserver.core.GlobalQuakeServer;
import gqserver.exception.RuntimeApplicationException;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GQServerSocket {

    private SocketStatus status;
    private ExecutorService serverExec;

    private List<ServerClient> clients;

    public GQServerSocket() {
        status = SocketStatus.IDLE;
        serverExec = Executors.newSingleThreadExecutor();
        clients = new ArrayList<>();
    }

    public synchronized void run(String ip, int port) throws IOException {
        status = SocketStatus.OPENING;
        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ip, port));
            serverExec.submit(() -> serverThread(serverSocket));
            status = SocketStatus.RUNNING;
        } catch (IOException e) {
            status = SocketStatus.IDLE;
            throw new RuntimeApplicationException("Unable to open server", e);
        }
    }

    private Runnable serverThread(ServerSocket serverSocket) {
        return new Runnable() {
            @Override
            public void run() {
                while (serverSocket.isBound()) {
                    try {
                        Socket socket = serverSocket.accept();
                        clients.add(new ServerClient(socket));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                onClose();
            }
        };
    }

    private void onClose() {
        status = SocketStatus.IDLE;
    }

    public SocketStatus getStatus() {
        return status;
    }
}
