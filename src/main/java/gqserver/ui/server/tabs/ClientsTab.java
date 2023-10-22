package gqserver.ui.server.tabs;

import gqserver.core.GlobalQuakeServer;
import gqserver.events.GlobalQuakeEventAdapter;
import gqserver.events.specific.ClientJoinedEvent;
import gqserver.events.specific.ClientLeftEvent;
import gqserver.ui.server.table.GQTable;
import gqserver.ui.server.table.model.ClientsTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientsTab extends JPanel {

    public ClientsTab(){
        setLayout(new BorderLayout());

        ClientsTableModel model;
        add(new JScrollPane(new GQTable<>(
                model = new ClientsTableModel(GlobalQuakeServer.instance.getServerSocket().getClients()))));

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(model::applyFilter, 0,1, TimeUnit.SECONDS);

        GlobalQuakeServer.instance.getEventHandler().registerEventListener(new GlobalQuakeEventAdapter(){
            @Override
            public void onClientLeave(ClientLeftEvent clientLeftEvent) {
                model.applyFilter();
            }

            @Override
            public void onClientJoin(ClientJoinedEvent clientJoinedEvent) {
                model.applyFilter();
            }
        });
    }

}
