package gqserver.ui.server.tabs;

import gqserver.core.GlobalQuakeServer;
import gqserver.core.earthquake.data.Cluster;
import gqserver.core.earthquake.data.Earthquake;
import gqserver.events.GlobalQuakeEventAdapter;
import gqserver.events.specific.ClusterCreateEvent;
import gqserver.events.specific.QuakeCreateEvent;
import gqserver.events.specific.QuakeRemoveEvent;
import gqserver.events.specific.QuakeUpdateEvent;
import gqserver.ui.server.table.GQTable;
import gqserver.ui.server.table.model.ClusterTableModel;
import gqserver.ui.server.table.model.EarthquakeTableModel;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClustersTab extends JPanel {

    public ClustersTab(){
        setLayout(new BorderLayout());

        ClusterTableModel model;
        add(new JScrollPane(new GQTable<Cluster>(
                model = new ClusterTableModel(GlobalQuakeServer.instance.getClusterAnalysis().getClusters()))));

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> model.applyFilter(), 0,1, TimeUnit.SECONDS);
    }

}
