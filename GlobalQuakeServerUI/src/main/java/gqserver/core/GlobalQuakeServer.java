package gqserver.core;

import gqserver.core.earthquake.ClusterAnalysis;
import gqserver.core.earthquake.EarthquakeAnalysis;
import gqserver.core.archive.EarthquakeArchive;
import gqserver.core.station.GlobalStationManager;
import gqserver.database.StationDatabaseManager;
import gqserver.events.GlobalQuakeEventHandler;
import gqserver.server.GQServerSocket;

public class GlobalQuakeServer {

	private final GlobalQuakeRuntime globalQuakeRuntime;
	private final SeedlinkNetworksReader seedlinkNetworksReader;
	private final StationDatabaseManager stationDatabaseManager;
	private final ClusterAnalysis clusterAnalysis;
	private final EarthquakeAnalysis earthquakeAnalysis;
	private final EarthquakeArchive archive;

	private final GlobalQuakeEventHandler eventHandler;

	public static GlobalQuakeServer instance;

	private final GlobalStationManager globalStationManager;

	private final GQServerSocket serverSocket;

	public GlobalQuakeServer(StationDatabaseManager stationDatabaseManager) {
		instance = this;
		this.stationDatabaseManager = stationDatabaseManager;

		eventHandler = new GlobalQuakeEventHandler().runHandler();

		globalStationManager = new GlobalStationManager();

		earthquakeAnalysis = new EarthquakeAnalysis();
		clusterAnalysis = new ClusterAnalysis();

		archive = new EarthquakeArchive().loadArchive();

		globalQuakeRuntime = new GlobalQuakeRuntime();
		seedlinkNetworksReader = new SeedlinkNetworksReader();

		serverSocket = new GQServerSocket();
	}

	public void startRuntime(){
		globalStationManager.initStations(stationDatabaseManager);
		getGlobalQuakeRuntime().runThreads();
		seedlinkNetworksReader.run();
	}

	public void stopRuntime(){
		getGlobalQuakeRuntime().stop();
		getSeedlinkReader().stop();

		getEarthquakeAnalysis().getEarthquakes().clear();
		getClusterAnalysis().getClusters().clear();
		getStationManager().getStations().clear();
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

	public GQServerSocket getServerSocket() {
		return serverSocket;
	}
}
