package gqserver.api;

import java.io.IOException;
import java.io.Serializable;

public abstract class Packet implements Serializable {

    public void onServerReceive(ServerClient serverClient) throws IOException {}

    public void onClientReceive() {}

}
