package gqserver.core;

import gqserver.core.alert.AlertManager;
import gqserver.core.earthquake.ClusterAnalysis;
import gqserver.core.earthquake.data.Earthquake;
import gqserver.core.earthquake.EarthquakeAnalysis;
import gqserver.core.archive.EarthquakeArchive;
import gqserver.core.station.GlobalStationManager;
import gqserver.database.StationDatabaseManager;
import gqserver.events.GlobalQuakeEventHandler;
import gqserver.main.Main;
import gqserver.ui.globalquake.GlobalQuakeFrame;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GlobalQuake {

	private final GlobalQuakeRuntime globalQuakeRuntime;
	private final SeedlinkNetworksReader seedlinkNetworksReader;
	private final StationDatabaseManager stationDatabaseManager;
	private GlobalQuakeFrame globalQuakeFrame;
	private final ClusterAnalysis clusterAnalysis;
	private final EarthquakeAnalysis earthquakeAnalysis;
	private final AlertManager alertManager;
	private final EarthquakeArchive archive;

	private final GlobalQuakeEventHandler eventHandler;

	public static GlobalQuake instance;

	private final GlobalStationManager globalStationManager;

	public GlobalQuake(StationDatabaseManager stationDatabaseManager) {
		instance = this;
		this.stationDatabaseManager = stationDatabaseManager;

		eventHandler = new GlobalQuakeEventHandler().runHandler();

		globalStationManager = new GlobalStationManager();
		globalStationManager.initStations(stationDatabaseManager);

		earthquakeAnalysis = new EarthquakeAnalysis();
		clusterAnalysis = new ClusterAnalysis();

		alertManager = new AlertManager();
		archive = new EarthquakeArchive().loadArchive();

		globalQuakeRuntime = new GlobalQuakeRuntime();
		seedlinkNetworksReader = new SeedlinkNetworksReader();
	}

	public GlobalQuake runSeedlinkReader() {
		seedlinkNetworksReader.run();
		return this;
	}

	public GlobalQuake createFrame() {
		EventQueue.invokeLater(() -> {
			globalQuakeFrame = new GlobalQuakeFrame();
			globalQuakeFrame.setVisible(true);


			Main.getErrorHandler().setParent(globalQuakeFrame);

            globalQuakeFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
					for (Earthquake quake : getEarthquakeAnalysis().getEarthquakes()) {
						getArchive().archiveQuake(quake);
					}
                    getArchive().saveArchive();
                }
            });
		});
		return this;
	}

	public void startRuntime(){
		getGlobalQuakeRuntime().runThreads();
	}

	public ClusterAnalysis getClusterAnalysis() {
		return clusterAnalysis;
	}

	public EarthquakeAnalysis getEarthquakeAnalysis() {
		return earthquakeAnalysis;
	}

	public EarthquakeArchive getArchive() {
		return archive;
	}

	public GlobalStationManager getStationManager() {
		return globalStationManager;
	}

	public AlertManager getAlertManager() {
		return alertManager;
	}

	public GlobalQuakeRuntime getGlobalQuakeRuntime() {
		return globalQuakeRuntime;
	}

	public SeedlinkNetworksReader getSeedlinkReader() {
		return seedlinkNetworksReader;
	}

	public StationDatabaseManager getStationDatabaseManager() {
		return stationDatabaseManager;
	}

	public GlobalQuakeEventHandler getEventHandler() {
		return eventHandler;
	}

	public GlobalQuakeFrame getGlobalQuakeFrame() {
		return globalQuakeFrame;
	}
}
