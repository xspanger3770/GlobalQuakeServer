package gqserver.core;

import gqserver.core.alert.AlertManager;
import gqserver.core.earthquake.ClusterAnalysis;
import gqserver.core.earthquake.EarthquakeAnalysis;
import gqserver.core.archive.EarthquakeArchive;
import gqserver.core.station.GlobalStationManager;
import gqserver.database.StationDatabaseManager;
import gqserver.events.GlobalQuakeEventHandler;

public class GlobalQuakeServer {

	private final GlobalQuakeRuntime globalQuakeRuntime;
	private final SeedlinkNetworksReader seedlinkNetworksReader;
	private final StationDatabaseManager stationDatabaseManager;
	private final ClusterAnalysis clusterAnalysis;
	private final EarthquakeAnalysis earthquakeAnalysis;
	private final AlertManager alertManager;
	private final EarthquakeArchive archive;

	private final GlobalQuakeEventHandler eventHandler;

	public static GlobalQuakeServer instance;

	private final GlobalStationManager globalStationManager;

	public GlobalQuakeServer(StationDatabaseManager stationDatabaseManager) {
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

	public GlobalQuakeServer runSeedlinkReader() {
		seedlinkNetworksReader.run();
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

}
