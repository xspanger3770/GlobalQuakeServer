package gqserver.server;

import gqserver.api.Packet;
import gqserver.api.ServerApiInfo;
import gqserver.api.ServerClient;
import gqserver.api.packets.HandshakePacket;
import gqserver.api.packets.TerminationPacket;
import gqserver.core.GlobalQuakeServer;
import gqserver.events.specific.ClientJoinedEvent;
import gqserver.events.specific.ClientLeftEvent;
import gqserver.events.specific.ServerStatusChangedEvent;
import gqserver.exception.InvalidPacketException;
import gqserver.exception.RuntimeApplicationException;
import gqserver.exception.UnknownPacketException;
import gqserver.utils.monitorable.MonitorableCopyOnWriteArrayList;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;

public class GQServerSocket {

    private static final int HANDSHAKE_TIMEOUT = 10 * 1000;
    public static final int MAX_CLIENTS = 64;
    private static final int WATCHDOG_TIMEOUT = 60 * 1000;

    public static final int READ_TIMEOUT = WATCHDOG_TIMEOUT + 10 * 1000;
    private SocketStatus status;
    private ExecutorService serverExec;
    private ExecutorService handshakeService;
    private ExecutorService readerService;
    private ScheduledExecutorService clientsWatchdog;
    private final List<ServerClient> clients;

    private volatile ServerSocket lastSocket;
    private final Object joinMutex = new Object();

    public GQServerSocket() {
        status = SocketStatus.IDLE;
        clients = new MonitorableCopyOnWriteArrayList<>();
    }

    public void run(String ip, int port) throws IOException {
        serverExec = Executors.newSingleThreadExecutor();
        handshakeService = Executors.newCachedThreadPool();
        readerService = Executors.newCachedThreadPool();
        clientsWatchdog = Executors.newSingleThreadScheduledExecutor();

        setStatus(SocketStatus.OPENING);
        try {
            lastSocket = new ServerSocket();
            lastSocket.bind(new InetSocketAddress(ip, port));
            serverExec.submit(this::runAccept);
            clientsWatchdog.scheduleAtFixedRate(this::checkClients, 0, 10, TimeUnit.SECONDS);
            setStatus(SocketStatus.RUNNING);
        } catch (IOException e) {
            setStatus(SocketStatus.IDLE);
            throw new RuntimeApplicationException("Unable to open server", e);
        }
    }

    private void checkClients() {
        for (Iterator<ServerClient> iterator = clients.iterator(); iterator.hasNext(); ) {
            ServerClient client = iterator.next();
            if(!client.isConnected() || System.currentTimeMillis() - client.getLastHeartbeat() > WATCHDOG_TIMEOUT){
                try {
                    client.destroy();
                } catch (IOException e) {
                    Logger.error(e);
                }
                iterator.remove();
                GlobalQuakeServer.instance.getEventHandler().fireEvent(new ClientLeftEvent(client));
                Logger.info("Client #%d disconnected due to timeout".formatted(client.getID()));
            }
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

            synchronized (joinMutex) {
                if (clients.size() >= MAX_CLIENTS) {
                    client.sendPacket(new TerminationPacket("Server is full!"));
                    client.destroy();
                } else {
                    Logger.info("Client #%d handshake successfull".formatted(client.getID()));
                    readerService.submit(new ClientReader(client));
                    clients.add(client);
                    GlobalQuakeServer.instance.getEventHandler().fireEvent(new ClientJoinedEvent(client));
                }
            }
        } catch (UnknownPacketException | InvalidPacketException e) {
            client.destroy();
            Logger.error(e);
        }
    }

    private void onClose() {
        clients.clear();
        if(!clientsWatchdog.isShutdown()){
            clientsWatchdog.shutdown();
        }
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

    public int getClientCount() {
        return clients.size();
    }

    public List<ServerClient> getClients() {
        return clients;
    }
}
