package gqserver.server;

import gqserver.api.Packet;
import gqserver.api.packets.TerminationPacket;
import gqserver.exception.UnknownPacketException;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerClient {

    private static final AtomicInteger nextID = new AtomicInteger(0);
    private final Socket socket;
    private final int id;

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public ServerClient(Socket socket) {
        this.socket = socket;
        this.id = nextID.getAndIncrement();
    }

    private ObjectInputStream getInputStream() throws IOException {
        if(inputStream == null) {
            inputStream = new ObjectInputStream(socket.getInputStream());
        }

        return inputStream;
    }

    private ObjectOutputStream getOutputStream() throws IOException {
        if(outputStream == null) {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        }

        return outputStream;
    }

    public Packet readPakcet() throws IOException, UnknownPacketException {
        try {
            Object obj = getInputStream().readObject();
            if(obj instanceof Packet){
                return (Packet) obj;
            }

            throw new UnknownPacketException("Received obj not instance of Packet!", null);
        }catch(ClassNotFoundException e){
            throw new UnknownPacketException(e.getMessage(), e);
        }
    }


    public void sendPacket(Packet packet) throws IOException{
        getOutputStream().writeObject(packet);
    }

    public void destroy() throws IOException {
        socket.close();
    }

    public int getID() {
        return id;
    }
}
