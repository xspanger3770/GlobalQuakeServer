package gqserver.api.packets;

import gqserver.api.Packet;

public class TerminationPacket extends Packet {
    private final String cause;

    public TerminationPacket(String cause) {
        this.cause = cause;
    }

    public String getCause() {
        return cause;
    }
}
