package gqserver.ui.server.tabs;

import gqserver.core.GlobalQuakeServer;
import gqserver.core.earthquake.data.Earthquake;
import gqserver.events.GlobalQuakeEventAdapter;
import gqserver.events.specific.*;
import gqserver.ui.server.table.GQTable;
import gqserver.ui.server.table.model.EarthquakeTableModel;

import javax.swing.*;
import java.awt.*;

public class EarthquakesTab extends JPanel {

    public EarthquakesTab(){
        setLayout(new BorderLayout());

        EarthquakeTableModel model;
        add(new JScrollPane(new GQTable<>(
                model = new EarthquakeTableModel(GlobalQuakeServer.instance.getEarthquakeAnalysis().getEarthquakes()))));

        GlobalQuakeServer.instance.getEventHandler().registerEventListener(new GlobalQuakeEventAdapter(){
            @Override
            public void onQuakeUpdate(QuakeUpdateEvent event) {
                model.applyFilter();
            }

            @Override
            public void onQuakeRemove(QuakeRemoveEvent quakeRemoveEvent) {
                model.applyFilter();
            }

            @Override
            public void onQuakeCreate(QuakeCreateEvent event) {
                model.applyFilter();
            }
        });
    }

}
