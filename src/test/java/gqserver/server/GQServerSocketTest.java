package gqserver.server;

import gqserver.exception.RuntimeApplicationException;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.IOException;

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

}