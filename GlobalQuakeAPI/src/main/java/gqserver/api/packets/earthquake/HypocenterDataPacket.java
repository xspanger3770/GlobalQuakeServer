package gqserver.api.packets.earthquake;

import gqserver.api.Packet;
import gqserver.api.data.HypocenterData;

public class HypocenterDataPacket extends Packet {

    private final HypocenterData data;

    public HypocenterDataPacket(HypocenterData data){
        this.data = data;
    }

    public HypocenterData getData() {
        return data;
    }
}
