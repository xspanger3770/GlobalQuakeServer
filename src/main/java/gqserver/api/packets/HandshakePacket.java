package gqserver.api.packets;

import gqserver.api.Packet;

public class HandshakePacket extends Packet {

    private final int compatVersion;

    public HandshakePacket(int compatVersion){
        this.compatVersion = compatVersion;
    }

    public int getCompatVersion() {
        return compatVersion;
    }
}
