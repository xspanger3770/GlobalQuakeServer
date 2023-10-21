package gqserver.server;

import gqserver.api.Packet;
import gqserver.api.ServerApiInfo;
import gqserver.api.packets.HandshakePacket;
import gqserver.api.packets.TerminationPacket;
import gqserver.core.GlobalQuakeServer;
import gqserver.events.specific.ServerStatusChangedEvent;
import gqserver.exception.InvalidPacketException;
import gqserver.exception.RuntimeApplicationException;
import gqserver.exception.UnknownPacketException;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.ObjectInputStream;
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

    private void handshake(ServerClient client) throws IOException{
        try {
            Packet packet = client.readPakcet();
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

            Logger.info("Client #%d handshake succesfull".formatted(client.getID()));

            clients.add(client);
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
            System.err.println("CLOSE "+lastSocket.isClosed());
        }
    }

    private void runAccept() {
        while (lastSocket.isBound() && !lastSocket.isClosed()) {
            try {
                Socket socket = lastSocket.accept();
                handshakeService.submit(() -> {
                    try {
                        handshake(new ServerClient(socket));
                    } catch (IOException e) {
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
