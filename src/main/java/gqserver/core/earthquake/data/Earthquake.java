package gqserver.core.earthquake.data;

import gqserver.core.GlobalQuakeServer;
import gqserver.core.alert.Warnable;
import gqserver.events.specific.ShakeMapCreatedEvent;
import gqserver.intensity.ShakeMap;
import gqserver.regions.RegionUpdater;
import gqserver.regions.Regional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Earthquake implements Regional, Warnable {

	private final ExecutorService shakemapExecutor;
	private final UUID uuid;
	private long lastUpdate;
	private final Cluster cluster;
	public int nextReportEventCount;
	private String region;

	private final RegionUpdater regionUpdater;
	volatile private ShakeMap shakemap;
	private double lastLat;
	private double lastLon;

	public Earthquake(Cluster cluster, double lat, double lon, double depth, long origin) {
		this.uuid = UUID.randomUUID();
		this.cluster = cluster;
		this.regionUpdater = new RegionUpdater(this);

		this.lastUpdate = System.currentTimeMillis();
		shakemapExecutor = Executors.newSingleThreadExecutor();
	}

	public void uppdateRegion(){
		regionUpdater.updateRegion();
	}

	public double getMag() {
		Hypocenter hyp = getHypocenter();
		return hyp == null ? 0.0 : hyp.magnitude;
	}

	public double getPct() {
		Hypocenter hyp = getHypocenter();
		return hyp == null ? 0.0 : 100.0 * hyp.getCorrectness();
	}

	public int getRevisionID() {
		Hypocenter hyp = getHypocenter();
		return hyp == null ? 0 : cluster.revisionID;
	}

	public long getLastUpdate() {
		return lastUpdate;
	}

	public double getDepth() {
		Hypocenter hyp = getHypocenter();
		return hyp == null ? 0.0 : hyp.depth;
	}

	public double getLat() {
		Hypocenter hyp = getHypocenter();
		return hyp == null ? 0.0 : hyp.lat;
	}

	public double getLon() {
		Hypocenter hyp = getHypocenter();
		return hyp == null ? 0.0 : hyp.lon;
	}

	public long getOrigin() {
		Hypocenter hyp = getHypocenter();
		return hyp == null ? 0L : hyp.origin;
	}

	public void update(Earthquake newEarthquake) {
		if (getLat() != lastLat || getLon() != lastLon) {
			regionUpdater.updateRegion();
		}

		lastLat = getLat();
		lastLon = getLon();
		this.lastUpdate = System.currentTimeMillis();
	}

	public Cluster getCluster() {
		return cluster;
	}

	public Hypocenter getHypocenter(){
		return getCluster().getPreviousHypocenter();
	}

	@Override
	public String getRegion() {
		return region;
	}

	@Override
	public void setRegion(String newRegion) {
		this.region = newRegion;
	}

	@Override
	public String toString() {
		return "Earthquake{" +
				"uuid=" + uuid +
				", lastUpdate=" + lastUpdate +
				", nextReportEventCount=" + nextReportEventCount +
				", region='" + region + '\'' +
				", lastLat=" + lastLat +
				", lastLon=" + lastLon +
				'}';
	}

	@SuppressWarnings("unused")
	@Override
	public double getWarningLat() {
		return getLat();
	}

	@SuppressWarnings("unused")
	@Override
	public double getWarningLon() {
		return getLon();
	}

    public void updateShakemap(Hypocenter hypocenter) {
		shakemapExecutor.submit(() -> {
            double mag = hypocenter.magnitude;
            shakemap = new ShakeMap(hypocenter, mag < 5.2 ? 6 : mag < 6.4 ? 5 : mag < 8.5 ? 4 : 3);
            if(GlobalQuakeServer.instance != null) {
                GlobalQuakeServer.instance.getEventHandler().fireEvent(new ShakeMapCreatedEvent(Earthquake.this));
            }
        });
	}

	public ShakeMap getShakemap() {
		return shakemap;
	}

	public LocalDateTime getOriginDate() {
		return Instant.ofEpochMilli(getOrigin()).atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	public UUID getUuid() {
		return uuid;
	}
}
