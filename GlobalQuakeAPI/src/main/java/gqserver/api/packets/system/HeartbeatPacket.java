package gqserver.api.packets.system;

import gqserver.api.Packet;
import gqserver.api.ServerClient;

import java.io.IOException;

public class HeartbeatPacket extends Packet {

    @Override
    public void onServerReceive(ServerClient serverClient) throws IOException {
        serverClient.noteHeartbeat();
        serverClient.sendPacket(new HeartbeatPacket());
    }
}
