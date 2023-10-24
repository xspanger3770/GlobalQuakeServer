package gqserver.server;

import globalquake.core.GlobalQuake;
import globalquake.core.database.StationDatabaseManager;
import gqserver.events.GlobalQuakeServerEventHandler;
import gqserver.main.Main;

public class GlobalQuakeServer extends GlobalQuake {

    private final GQServerSocket serverSocket;

    public static GlobalQuakeServer instance;

    private final GlobalQuakeServerEventHandler serverEventHandler;

    public GlobalQuakeServer(StationDatabaseManager stationDatabaseManager) {
        super(stationDatabaseManager, Main.getErrorHandler(), Main.MAIN_FOLDER);
        instance = this;
        serverSocket = new GQServerSocket();
        this.serverEventHandler = new GlobalQuakeServerEventHandler().runHandler();
    }

    public GQServerSocket getServerSocket() {
        return serverSocket;
    }

    public GlobalQuakeServerEventHandler getServerEventHandler() {
        return serverEventHandler;
    }
}
