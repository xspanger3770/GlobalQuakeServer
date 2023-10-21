package gqserver.server;

import gqserver.api.ServerApiInfo;
import gqserver.api.packets.HandshakePacket;
import gqserver.exception.RuntimeApplicationException;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.Assert.*;

public class GQServerSocketTest {

    @Test
    public void openTest() throws IOException {
        GQServerSocket socket = new GQServerSocket();
        assertThrows(RuntimeApplicationException.class, new ThrowingRunnable() {
            @Override
            public void run() throws Throwable {
                socket.run("invalidaddress", 42);
            }
        });

    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Socket socket  = new Socket();
        socket.connect(new InetSocketAddress("0.0.0.0", 12345));


        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.writeObject(new HandshakePacket(ServerApiInfo.COMPATIBILITY_VERSION));

        Thread.sleep(120 * 1000);
    }

}