package gqserver.server;

import gqserver.core.GlobalQuakeServer;
import gqserver.events.specific.ServerStatusChangedEvent;
import gqserver.exception.RuntimeApplicationException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GQServerSocket {

    private static final int TIMEOUT = 60 * 1000;
    private SocketStatus status;
    private final ExecutorService serverExec;
    private final ExecutorService handshakeService;
    private final List<ServerClient> clients;

    public GQServerSocket() {
        status = SocketStatus.IDLE;
        serverExec = Executors.newSingleThreadExecutor();
        handshakeService = Executors.newSingleThreadExecutor();
        clients = new ArrayList<>();
    }

    public synchronized void run(String ip, int port) throws IOException {
        setStatus(SocketStatus.OPENING);
        try (ServerSocket serverSocket = new ServerSocket()){
            serverSocket.bind(new InetSocketAddress(ip, port));
            serverSocket.setSoTimeout(TIMEOUT);
            serverExec.submit(() -> serverThread(serverSocket));
            setStatus(SocketStatus.RUNNING);
        } catch (IOException e) {
            setStatus(SocketStatus.IDLE);
            throw new RuntimeApplicationException("Unable to open server", e);
        }
    }

    private Runnable serverThread(ServerSocket serverSocket) {
        return () -> {
            while (serverSocket.isBound()) {
                try {
                    Socket socket = serverSocket.accept();
                    ServerClient client;
                    clients.add(client = new ServerClient(socket));
                    handshakeService.submit(() -> handshake(client));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            onClose();
        };
    }

    private void handshake(ServerClient client) {

    }

    private void onClose() {
        setStatus(SocketStatus.IDLE);
        if(GlobalQuakeServer.instance != null){
            GlobalQuakeServer.instance.getEventHandler().fireEvent(new ServerStatusChangedEvent());
        }
    }

    public void setStatus(SocketStatus status) {
        this.status = status;

    }

    public SocketStatus getStatus() {
        return status;
    }
}
