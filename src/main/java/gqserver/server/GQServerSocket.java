package gqserver.server;

import gqserver.api.Packet;
import gqserver.api.ServerApiInfo;
import gqserver.api.ServerClient;
import gqserver.api.packets.HandshakePacket;
import gqserver.api.packets.TerminationPacket;
import gqserver.core.GlobalQuakeServer;
import gqserver.events.specific.ServerStatusChangedEvent;
import gqserver.exception.InvalidPacketException;
import gqserver.exception.RuntimeApplicationException;
import gqserver.exception.UnknownPacketException;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GQServerSocket {

    private static final int HANDSHAKE_TIMEOUT = 10 * 1000;
    private static final int MAX_CLIENTS = 64;
    private SocketStatus status;
    private ExecutorService serverExec;
    private ExecutorService handshakeService;
    private ExecutorService readerService;
    private final Queue<ServerClient> clients;

    private volatile ServerSocket lastSocket;

    public GQServerSocket() {
        status = SocketStatus.IDLE;
        clients = new ConcurrentLinkedQueue<>();
    }

    public void run(String ip, int port) throws IOException {
        serverExec = Executors.newSingleThreadExecutor();
        handshakeService = Executors.newCachedThreadPool();
        readerService = Executors.newCachedThreadPool();

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

    private void handshake(ServerClient client) throws IOException{
        try {
            Packet packet = client.readPacket();
            if(packet instanceof HandshakePacket){
                HandshakePacket handshakePacket = (HandshakePacket) packet;
                if(handshakePacket.getCompatVersion() != ServerApiInfo.COMPATIBILITY_VERSION){
                    client.sendPacket(new TerminationPacket("Your client version is not compatible with the server!"));
                    throw new InvalidPacketException("Client's version is not compatible %d != %d"
                            .formatted(handshakePacket.getCompatVersion(), ServerApiInfo.COMPATIBILITY_VERSION));
                }
            }else {
                throw new InvalidPacketException("Received packet is not handshake!");
            }

            if(clients.size() >= MAX_CLIENTS){
                client.sendPacket(new TerminationPacket("Server is full!"));
                client.destroy();
            } else {
                Logger.info("Client #%d handshake successfull".formatted(client.getID()));
                readerService.submit(new ClientReader(client));
                clients.add(client);
            }
        } catch (UnknownPacketException | InvalidPacketException e) {
            client.destroy();
            Logger.error(e);
        }
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
        }
    }

    private void runAccept() {
        while (lastSocket.isBound() && !lastSocket.isClosed()) {
            try {
                lastSocket.setSoTimeout(0); // we can wait for clients forever
                Socket socket = lastSocket.accept();
                Logger.info("A new client is joining...");
                socket.setSoTimeout(HANDSHAKE_TIMEOUT);
                handshakeService.submit(() -> {
                    ServerClient client = new ServerClient(socket);
                    Logger.info("Performing handshake for client #%d".formatted(client.getID()));
                    try {
                        handshake(client);
                    } catch (IOException e) {
                        Logger.error("Failure when accepting client #%d".formatted(client.getID()));
                        Logger.error(e);
                    }
                });
            } catch (IOException e) {
                break;
            }
        }

        onClose();
    }
}
