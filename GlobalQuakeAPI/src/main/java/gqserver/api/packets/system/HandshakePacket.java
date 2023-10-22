package gqserver.api.packets.system;

import gqserver.api.Packet;
import gqserver.api.data.ServerClientConfig;

public class HandshakePacket extends Packet {

    private final int compatVersion;
    private final ServerClientConfig clientConfig;

    public HandshakePacket(int compatVersion, ServerClientConfig clientConfig){
        this.compatVersion = compatVersion;
        this.clientConfig = clientConfig;
    }

    public int getCompatVersion() {
        return compatVersion;
    }

    public ServerClientConfig getClientConfig() {
        return clientConfig;
    }
}
