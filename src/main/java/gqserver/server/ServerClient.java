package gqserver.server;

import java.net.Socket;

public class ServerClient {
    private final Socket socket;

    public ServerClient(Socket socket) {
        this.socket = socket;
    }
}
