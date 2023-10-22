package gqserver.api;

import java.io.Serializable;

public abstract class Packet implements Serializable {

    public void onServerReceive(ServerClient serverClient) {}

    public void onClientReceive() {}

}
