package globalquake.core;

import globalquake.core.database.SeedlinkNetwork;
import globalquake.core.database.SeedlinkStatus;
import globalquake.core.station.AbstractStation;
import globalquake.core.station.GlobalStation;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.seedlink.SeedlinkPacket;
import edu.sc.seis.seisFile.seedlink.SeedlinkReader;
import org.tinylog.Logger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SeedlinkNetworksReader {

	protected static final int RECONNECT_DELAY = 10;
	private Instant lastData;

    private long lastReceivedRecord;

	private ExecutorService seedlinkReaderService;

	private final Queue<SeedlinkReader> activeReaders = new ConcurrentLinkedQueue<>();

	public static void main(String[] args) throws Exception{
		SeedlinkReader reader = new SeedlinkReader("rtserve.iris.washington.edu", 18000);
		reader.select("AK", "D25K", "", "BHZ");
		reader.startData();

		SortedSet<DataRecord> set = new TreeSet<>(Comparator.comparing(dataRecord -> dataRecord.getStartBtime().toInstant().toEpochMilli()));

		while(reader.hasNext() && set.size() < 10){
			SeedlinkPacket pack = reader.readPacket();
			DataRecord dataRecord = pack.getMiniSeed();
			System.out.println(pack.getMiniSeed().getStartTime()+" - "+pack.getMiniSeed().getLastSampleTime()+" x "+pack.getMiniSeed().getEndTime()+" @ "+pack.getMiniSeed().getSampleRate());
			System.out.println(pack.getMiniSeed().getControlHeader().getSequenceNum());
			if(!set.add(dataRecord)){
				System.out.println("ERR ALREADY CONTAINS");
			}
		}

		reader.close();
		for(DataRecord dataRecord : set){
			System.err.println(dataRecord.getStartTime()+" - "+dataRecord.getLastSampleTime()+" x "+dataRecord.getEndTime()+" @ "+dataRecord.getSampleRate());
			System.err.println(dataRecord.oneLineSummary());
		}
	}

	public void run() {
		createCache();
		seedlinkReaderService = Executors.newCachedThreadPool();
		GlobalQuake.instance.getStationDatabaseManager().getStationDatabase().getDatabaseReadLock().lock();
		try{
			GlobalQuake.instance.getStationDatabaseManager().getStationDatabase().getSeedlinkNetworks().forEach(
					seedlinkServer -> seedlinkReaderService.submit(() -> runSeedlinkThread(seedlinkServer, RECONNECT_DELAY)));
		} finally {
			GlobalQuake.instance.getStationDatabaseManager().getStationDatabase().getDatabaseReadLock().unlock();
		}
	}

	private final Map<String, GlobalStation> stationCache = new HashMap<>();

	private void createCache() {
		for (AbstractStation s : GlobalQuake.instance.getStationManager().getStations()) {
			if (s instanceof GlobalStation) {
				stationCache.put("%s %s".formatted(s.getNetworkCode(), s.getStationCode()), (GlobalStation) s);
			}
		}
	}
	private void runSeedlinkThread(SeedlinkNetwork seedlinkNetwork, int reconnectDelay) {
		seedlinkNetwork.status = SeedlinkStatus.CONNECTING;
		seedlinkNetwork.connectedStations = 0;

		SeedlinkReader reader = null;
		try {
			Logger.info("Connecting to seedlink server \"" + seedlinkNetwork.getHost() + "\"");
			reader = new SeedlinkReader(seedlinkNetwork.getHost(), seedlinkNetwork.getPort(), 90, false);
			activeReaders.add(reader);

			reader.sendHello();

			reconnectDelay = RECONNECT_DELAY; // if connect succeeded then reset the delay
			boolean first = true;

			for (AbstractStation s : GlobalQuake.instance.getStationManager().getStations()) {
				if (s.getSeedlinkNetwork() != null && s.getSeedlinkNetwork().equals(seedlinkNetwork)) {
					Logger.trace("Connecting to %s %s %s %s [%s]".formatted(s.getStationCode(), s.getNetworkCode(), s.getChannelName(), s.getLocationCode(), seedlinkNetwork.getName()));
					if(!first) {
						reader.sendCmd("DATA");
					} else{
						first = false;
					}
					reader.select(s.getNetworkCode(), s.getStationCode(), s.getLocationCode(),
							s.getChannelName());
					seedlinkNetwork.connectedStations++;
				}
			}

			if(seedlinkNetwork.connectedStations == 0){
				Logger.info("No stations connected to "+seedlinkNetwork.getName());
				seedlinkNetwork.status = SeedlinkStatus.DISCONNECTED;
				return;
			}

			reader.startData();
			seedlinkNetwork.status = SeedlinkStatus.RUNNING;

			while (reader.hasNext()) {
				SeedlinkPacket slp = reader.readPacket();
				try {
					newPacket(slp.getMiniSeed());
				} catch (Exception e) {
					Logger.error(e);
				}
			}

			reader.close();
		} catch (Exception e) {
			Logger.error(e);
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception ex) {
					Logger.error(ex);
				}
			}
		}finally{
			if(reader != null){
				activeReaders.remove(reader);
			}
		}

		seedlinkNetwork.status = SeedlinkStatus.DISCONNECTED;
		seedlinkNetwork.connectedStations = 0;
		Logger.warn(seedlinkNetwork.getHost() + " Disconnected, Reconnecting after " + reconnectDelay
				+ " seconds...");

		try {
			Thread.sleep(reconnectDelay * 1000L);
			if(reconnectDelay < 60 * 5) {
				reconnectDelay *= 2;
			}
		} catch (InterruptedException ignored) {
			Logger.warn("Thread interrupted, nothing will happen");
			return;
		}

		int finalReconnectDelay = reconnectDelay;
		seedlinkReaderService.submit(() -> runSeedlinkThread(seedlinkNetwork, finalReconnectDelay));
	}

	private void newPacket(DataRecord dr) {
		if (lastData == null || dr.getLastSampleBtime().toInstant().isAfter(lastData)) {
			lastData = dr.getLastSampleBtime().toInstant();
		}

		String network = dr.getHeader().getNetworkCode().replaceAll(" ", "");
		String station = dr.getHeader().getStationIdentifier().replaceAll(" ", "");
		var globalStation = stationCache.get("%s %s".formatted(network, station));
		if(globalStation == null){
			Logger.trace("Warning! Seedlink sent data for %s %s, but that was never selected!!!".formatted(network, station));
		}else {
			globalStation.addRecord(dr);
		}
	}

    public long getLastReceivedRecord() {
        return lastReceivedRecord;
    }

    public void logRecord(long time) {
        if (time > lastReceivedRecord && time <= System.currentTimeMillis()) {
            lastReceivedRecord = time;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
	public void stop() {
		if(seedlinkReaderService != null) {
			System.err.println("INT SDL");
			seedlinkReaderService.shutdownNow();
            for (Iterator<SeedlinkReader> iterator = activeReaders.iterator(); iterator.hasNext(); ) {
                SeedlinkReader reader = iterator.next();
                reader.close();
            	iterator.remove();
			}
			try {
				seedlinkReaderService.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Logger.error(e);
			}

			System.err.println("INT SDL DONE");
		}
		stationCache.clear();
	}
}
