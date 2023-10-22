package gqserver.ui.server;

import gqserver.core.GlobalQuakeServer;
import gqserver.events.GlobalQuakeEventAdapter;
import gqserver.events.specific.ServerStatusChangedEvent;
import gqserver.exception.RuntimeApplicationException;
import gqserver.main.Main;
import gqserver.server.SocketStatus;
import gqserver.ui.server.tabs.SeedlinksTab;
import gqserver.ui.server.tabs.StatusTab;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ServerStatusPanel extends JPanel {
    private JButton controlButton;
    private JLabel statusLabel;
    private JTextField addressField;
    private JTextField portField;

    public ServerStatusPanel(DatabaseMonitorFrame databaseMonitorFrame) {
        setLayout(new BorderLayout());

        add(createTopPanel(), BorderLayout.NORTH);
        add(createMiddlePanel(), BorderLayout.CENTER);
    }

    private Component createMiddlePanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Status", new StatusTab());
        tabbedPane.addTab("Seedlinks", new SeedlinksTab());
        tabbedPane.addTab("Stations", new JPanel());
        tabbedPane.addTab("Clients", new JPanel());
        tabbedPane.addTab("Earthquakes", new JPanel());
        tabbedPane.addTab("Clusters", new JPanel());

        return tabbedPane;
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));

        JPanel addressPanel = new JPanel(new GridLayout(2,1));
        addressPanel.setBorder(BorderFactory.createTitledBorder("Server address"));

        JPanel ipPanel = new JPanel();
        ipPanel.setLayout(new BoxLayout(ipPanel, BoxLayout.X_AXIS));
        ipPanel.add(new JLabel("IP Address: "));
        ipPanel.add(addressField = new JTextField("0.0.0.0",20));

        addressPanel.add(ipPanel);

        JPanel portPanel = new JPanel();
        portPanel.setLayout(new BoxLayout(portPanel, BoxLayout.X_AXIS));
        portPanel.add(new JLabel("Port: "));
        portPanel.add(portField = new JTextField("12345",20));

        addressPanel.add(portPanel);

        topPanel.add(addressPanel);

        JPanel controlPanel = new JPanel(new GridLayout(2,1));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Control Panel"));

        controlPanel.add(statusLabel = new JLabel("Status: Idle"));
        controlPanel.add(controlButton = new JButton("Start Server"));

        GlobalQuakeServer.instance.getEventHandler().registerEventListener(new GlobalQuakeEventAdapter(){
            @Override
            public void onServerStatusChanged(ServerStatusChangedEvent event) {
                switch (event.status()){
                    case IDLE -> {
                        addressField.setEnabled(true);
                        portField.setEnabled(true);
                        controlButton.setEnabled(true);
                        controlButton.setText("Start Server");
                    }
                    case OPENING -> {
                        addressField.setEnabled(false);
                        portField.setEnabled(false);
                        controlButton.setEnabled(false);
                        controlButton.setText("Start Server");
                    }
                    case RUNNING -> {
                        addressField.setEnabled(false);
                        portField.setEnabled(false);
                        controlButton.setEnabled(true);
                        controlButton.setText("Stop Server");
                    }
                }
                statusLabel.setText("Status: %s".formatted(event.status()));
            }
        });

        controlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SocketStatus status = GlobalQuakeServer.instance.getServerSocket().getStatus();
                if(status == SocketStatus.IDLE){
                    try {
                        GlobalQuakeServer.instance.getServerSocket().run(addressField.getText(), Integer.parseInt(portField.getText()));
                        GlobalQuakeServer.instance.startRuntime();
                    } catch(Exception e){
                        Main.getErrorHandler().handleException(new RuntimeApplicationException("Failed to start server", e));
                    }
                } else if(status == SocketStatus.RUNNING) {
                    if(confirm("Are you sure you want to close the server?")) {
                        try {
                            GlobalQuakeServer.instance.getServerSocket().stop();
                            GlobalQuakeServer.instance.stopRuntime();
                        } catch (IOException e) {
                            Main.getErrorHandler().handleException(new RuntimeApplicationException("Failed to stop server", e));
                        }
                    }
                }
            }
        });

        topPanel.add(controlPanel);
        return topPanel;
    }

    private boolean confirm(String s) {
        return JOptionPane.showConfirmDialog(this, s, "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    private JPanel wrap(JPanel target) {
        JPanel panel = new JPanel();
        panel.add(target);
        return panel;
    }
}
