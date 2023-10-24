package globalquake.core;

import globalquake.core.archive.EarthquakeArchive;
import globalquake.core.database.StationDatabaseManager;
import globalquake.core.earthquake.ClusterAnalysis;
import globalquake.core.earthquake.EarthquakeAnalysis;
import globalquake.core.events.GlobalQuakeEventHandler;
import globalquake.core.exception.ApplicationErrorHandler;
import globalquake.core.station.GlobalStationManager;

import java.io.File;

public class GlobalQuake {

	protected GlobalQuakeRuntime globalQuakeRuntime;
	protected SeedlinkNetworksReader seedlinkNetworksReader;
	protected StationDatabaseManager stationDatabaseManager;
	protected ClusterAnalysis clusterAnalysis;
	protected EarthquakeAnalysis earthquakeAnalysis;
	protected EarthquakeArchive archive;

	protected GlobalQuakeEventHandler eventHandler;

	public static GlobalQuake instance;

	protected GlobalStationManager globalStationManager;

	protected ApplicationErrorHandler errorHandler;
	private final File mainFolder;

	public GlobalQuake(StationDatabaseManager stationDatabaseManager,
					   ApplicationErrorHandler errorHandler,
					   File mainFolder) {
		instance = this;
		this.mainFolder = mainFolder;
		this.errorHandler = errorHandler;
		this.stationDatabaseManager = stationDatabaseManager;

		eventHandler = new GlobalQuakeEventHandler().runHandler();

		globalStationManager = new GlobalStationManager();

		earthquakeAnalysis = new EarthquakeAnalysis();
		clusterAnalysis = new ClusterAnalysis();

		archive = new EarthquakeArchive().loadArchive();

		globalQuakeRuntime = new GlobalQuakeRuntime();
		seedlinkNetworksReader = new SeedlinkNetworksReader();
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

	public ApplicationErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public File getMainFolder() {
		return mainFolder;
	}
}
