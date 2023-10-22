package gqserver.ui.server.tabs;

import gqserver.core.GlobalQuakeServer;
import gqserver.database.SeedlinkNetwork;
import gqserver.ui.server.table.GQTable;
import gqserver.ui.server.table.model.SeedlinkStatusTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SeedlinksTab extends JPanel {

    public SeedlinksTab(){
        setLayout(new BorderLayout());

        SeedlinkStatusTableModel model;
        add(new JScrollPane(new GQTable<SeedlinkNetwork>(
                model = new SeedlinkStatusTableModel(GlobalQuakeServer.instance.getStationDatabaseManager().getStationDatabase().getSeedlinkNetworks()))));

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> model.applyFilter(), 0,1, TimeUnit.SECONDS);
    }

}
