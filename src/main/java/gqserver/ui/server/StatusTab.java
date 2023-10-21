package gqserver.ui.server;

import gqserver.core.GlobalQuakeServer;
import gqserver.events.GlobalQuakeEventAdapter;
import gqserver.events.specific.ClientJoinedEvent;
import gqserver.events.specific.ClientLeftEvent;
import gqserver.server.GQServerSocket;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StatusTab extends JPanel {

    private final JProgressBar clientsProgressBar;
    private final JProgressBar ramProgressBar;
    private static final double GB = 1024 * 1024 * 1024.0;
    private static final double MB = 1024 * 1024;

    public StatusTab() {
        setLayout(new GridLayout(2,1));


        long maxMem = Runtime.getRuntime().maxMemory();
        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        clientsProgressBar = new JProgressBar(JProgressBar.HORIZONTAL,0, GQServerSocket.MAX_CLIENTS);
        clientsProgressBar.setStringPainted(true);
        add(clientsProgressBar);

        ramProgressBar = new JProgressBar(JProgressBar.HORIZONTAL,0, (int) (maxMem / MB));
        ramProgressBar.setStringPainted(true);
        add(ramProgressBar);

        updateRamProgressBar();
        updateClientsProgressBar();

        GlobalQuakeServer.instance.getEventHandler().registerEventListener(new GlobalQuakeEventAdapter(){
            @Override
            public void onClientJoin(ClientJoinedEvent clientJoinedEvent) {
                updateClientsProgressBar();
            }

            @Override
            public void onClientLeave(ClientLeftEvent clientLeftEvent) {
                updateClientsProgressBar();
            }
        });

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::updateRamProgressBar, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void updateRamProgressBar() {
        long maxMem = Runtime.getRuntime().maxMemory();
        long usedMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        ramProgressBar.setString("RAM: %.2f / %.2f GB".formatted(usedMem / GB, maxMem / GB));
        ramProgressBar.setValue((int) (usedMem / MB));

        repaint();
    }

    private void updateClientsProgressBar() {
        int clients = GlobalQuakeServer.instance.getServerSocket().getClientCount();
        clientsProgressBar.setString("Clients: %d / %d".formatted(clients, GQServerSocket.MAX_CLIENTS));
        clientsProgressBar.setValue(clients);
        repaint();
    }

}
