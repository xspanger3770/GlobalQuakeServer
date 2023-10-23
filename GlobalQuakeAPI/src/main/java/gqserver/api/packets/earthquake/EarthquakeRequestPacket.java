package gqserver.api.packets.earthquake;

import gqserver.api.Packet;

import java.util.UUID;

public class EarthquakeRequestPacket extends Packet {

    private final UUID uuid;

    public EarthquakeRequestPacket(UUID uuid){
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }
}
