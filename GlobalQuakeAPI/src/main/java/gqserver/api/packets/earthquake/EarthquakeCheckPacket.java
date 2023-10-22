package gqserver.api.packets.earthquake;

import gqserver.api.Packet;
import gqserver.api.data.EarthquakeInfo;

public class EarthquakeCheckPacket extends Packet {

    private final EarthquakeInfo info;

    public EarthquakeCheckPacket(EarthquakeInfo info){
        this.info = info;
    }

    public EarthquakeInfo getInfo() {
        return info;
    }
}
