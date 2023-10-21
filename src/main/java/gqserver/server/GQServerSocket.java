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
    private ExecutorService serverExec;
    private ExecutorService handshakeService;
    private final List<ServerClient> clients;

    private volatile ServerSocket lastSocket;

    public GQServerSocket() {
        status = SocketStatus.IDLE;
        clients = new ArrayList<>();
    }

    public void run(String ip, int port) throws IOException {
        serverExec = Executors.newSingleThreadExecutor();
        handshakeService = Executors.newSingleThreadExecutor();

        setStatus(SocketStatus.OPENING);
        try {
            lastSocket = new ServerSocket();
            lastSocket.bind(new InetSocketAddress(ip, port));
            serverExec.submit(this::runAccept);
            setStatus(SocketStatus.RUNNING);
        } catch (IOException e) {
            setStatus(SocketStatus.IDLE);
            throw new RuntimeApplicationException("Unable to open server", e);
        }
    }

    private Runnable serverThread(ServerSocket serverSocket) {
        return null;
    }

    private void handshake(ServerClient client) {

    }

    private void onClose() {
        setStatus(SocketStatus.IDLE);
    }

    public void setStatus(SocketStatus status) {
        this.status = status;
        if(GlobalQuakeServer.instance != null){
            GlobalQuakeServer.instance.getEventHandler().fireEvent(new ServerStatusChangedEvent(status));
        }
    }

    public SocketStatus getStatus() {
        return status;
    }

    public void stop() throws IOException {
        if(lastSocket != null) {
            lastSocket.close();
            System.err.println("CLOSE "+lastSocket.isClosed());
        }
    }

    private void runAccept() {
        while (lastSocket.isBound() && !lastSocket.isClosed()) {
            try {
                Socket socket = lastSocket.accept();
                ServerClient client;
                clients.add(client = new ServerClient(socket));
                handshakeService.submit(() -> handshake(client));
            } catch (IOException e) {
                break;
            }
        }

        onClose();
    }
}
