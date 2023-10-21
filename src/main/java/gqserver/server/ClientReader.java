package gqserver.server;

import gqserver.api.Packet;
import gqserver.api.ServerClient;
import org.tinylog.Logger;

import java.io.IOException;

public class ClientReader implements Runnable {
    private final ServerClient client;

    public ClientReader(ServerClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        while(client.isConnected()){
            try {
                Packet packet = client.readPacket();
                packet.onServerReceive(client);
            } catch (Exception e) {
                Logger.error("Client #%d experienced a crash while reading!", client.getID());
                Logger.error(e);
                break;
            }
        }

        try {
            client.destroy();
        } catch (IOException e) {
            Logger.error(e);
        }
    }
}
