package gqserver.api.packets;

import gqserver.api.Packet;
import gqserver.api.ServerClient;

public class HeartbeatPacket extends Packet {

    @Override
    public void onServerReceive(ServerClient serverClient) {
        serverClient.noteHeartbeat();
    }
}
