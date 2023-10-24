package globalquake.core.earthquake;

import globalquake.core.GlobalQuake;
import globalquake.core.Settings;
import globalquake.core.events.specific.ClusterCreateEvent;
import globalquake.core.geo.taup.TauPTravelTimeCalculator;
import globalquake.core.intensity.IntensityTable;
import globalquake.core.station.AbstractStation;
import globalquake.core.earthquake.data.*;
import globalquake.core.analysis.Event;
import globalquake.core.station.NearbyStationDistanceInfo;
import globalquake.utils.GeoUtils;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClusterAnalysis {

    private static final int MIN_CLUSTER_SIZE = 4;
    private final ReadWriteLock clustersLock = new ReentrantReadWriteLock();

    private final Lock clustersReadLock = clustersLock.readLock();
    private final Lock clustersWriteLock = clustersLock.writeLock();

    private final List<Cluster> clusters;
    private final List<Earthquake> earthquakes;
    private final List<AbstractStation> stations;
    private final AtomicInteger nextClusterId = new AtomicInteger(0);

    private static final double MERGE_THRESHOLD = 0.45;

    public ClusterAnalysis(List<Earthquake> earthquakes, List<AbstractStation> stations) {
        this.earthquakes = earthquakes;
        this.stations = stations;
        clusters = new ArrayList<>();
    }

    public ClusterAnalysis() {
        this(GlobalQuake.instance.getEarthquakeAnalysis().getEarthquakes(), GlobalQuake.instance.getStationManager().getStations());
    }

    public Lock getClustersReadLock() {
        return clustersReadLock;
    }

    public void run() {
        clustersWriteLock.lock();
        try {
            clearSWaves();
            //assignEventsToExistingEarthquakeClusters(); VERY CONTROVERSIAL
            expandExistingClusters();
            createNewClusters();
            stealEvents();
            mergeClusters();
            updateClusters();
        } finally {
            clustersWriteLock.unlock();
        }
    }

    record EventIntensityInfo(Cluster cluster, AbstractStation station, double expectedIntensity){}

    private void stealEvents() {
        java.util.Map<Event, EventIntensityInfo> map = new HashMap<>();
        for(Cluster cluster : clusters) {
            if (cluster.getEarthquake() == null) {
                continue;
            }

            for (AbstractStation station : stations) {
                for (Event event : station.getAnalysis().getDetectedEvents()) {
                    if (event.isValid() && event.isSWave() && !couldBeArrival(event, cluster.getEarthquake(), true, false, true)) {
                        double distGC = GeoUtils.greatCircleDistance(event.getLatFromStation(), event.getLonFromStation(), cluster.getEarthquake().getLat(), cluster.getEarthquake().getLon());
                        double expectedIntensity = IntensityTable.getMaxIntensity(cluster.getEarthquake().getMag(), GeoUtils.gcdToGeo(distGC));
                        EventIntensityInfo eventIntensityInfo = new EventIntensityInfo(cluster, station, expectedIntensity);
                        EventIntensityInfo old = map.putIfAbsent(event, eventIntensityInfo);
                        if(old != null && eventIntensityInfo.expectedIntensity > old.expectedIntensity){
                            map.put(event, eventIntensityInfo);
                        }
                    }
                }
            }
        }

        // reassign
        for(var entry : map.entrySet()){
            Event event = entry.getKey();
            AbstractStation station = entry.getValue().station();
            Cluster cluster = entry.getValue().cluster();

            if(!cluster.getAssignedEvents().containsKey(station)){
                if(event.assignedCluster != null){
                    event.assignedCluster.getAssignedEvents().remove(station);
                }

                event.assignedCluster = cluster;
                cluster.getAssignedEvents().put(station, event);
            }
        }
    }

    private void clearSWaves() {
        for(Cluster cluster : clusters) {
            if(cluster.getEarthquake() == null){
                continue;
            }

            for (AbstractStation station : stations) {
                for (Event event : station.getAnalysis().getDetectedEvents()) {
                    if (event.isValid() && event.isSWave() && !couldBeSArrival(event, cluster.getEarthquake())) {
                        event.setAsSWave(false);
                    }
                }
            }
        }
    }

    private void mergeClusters() {
        for (Earthquake earthquake : earthquakes) {
            List<Cluster> toMerge = null;
            for (Cluster cluster : clusters) {
                if (earthquake.getCluster() == cluster) {
                    continue;
                }

                if (canMerge(earthquake, cluster)) {
                    if (toMerge == null) {
                        toMerge = new ArrayList<>();
                    }
                    toMerge.add(cluster);
                }
            }

            if (toMerge != null) {
                merge(earthquake, toMerge);
            }
        }
    }

    private void merge(Earthquake earthquake, List<Cluster> toMerge) {
        Cluster target = earthquake.getCluster();
        for (Cluster cluster : toMerge) {
            for (Entry<AbstractStation, Event> entry : cluster.getAssignedEvents().entrySet()) {
                if (target.getAssignedEvents().putIfAbsent(entry.getKey(), entry.getValue()) == null) {
                    entry.getValue().assignedCluster = target;
                }
            }

            if (cluster.getEarthquake() != null) {
                earthquakes.remove(cluster.getEarthquake());
            }
        }

        clusters.removeAll(toMerge);
    }

    private boolean canMerge(Earthquake earthquake, Cluster cluster) {
        if(cluster.getEarthquake() != null && cluster.getPreviousHypocenter() != null){
            int thatCorrect = cluster.getPreviousHypocenter().correctEvents;
            double dist = GeoUtils.greatCircleDistance(earthquake.getLat(), earthquake.getLon(), cluster.getEarthquake().getLat(), cluster.getEarthquake().getLon());
            double maxDist = 6000 / (1 + thatCorrect * 0.2);
            if(dist > maxDist){
                return false;
            }
        }
        int correct = 0;
        for (Event event : cluster.getAssignedEvents().values()) {
            if (couldBeArrival(event, earthquake, true, true, false)) {
                correct++;
            }
        }

        double pct = correct / (double) cluster.getAssignedEvents().size();

        return pct > MERGE_THRESHOLD;
    }

    @SuppressWarnings("unused")
    private void assignEventsToExistingEarthquakeClusters() {
        for (AbstractStation station : stations) {
            for (Event event : station.getAnalysis().getDetectedEvents()) {
                if (event.isValid() && event.getpWave() > 0 && event.assignedCluster == null) {
                    HashMap<Earthquake, Event> map = new HashMap<>();

                    for (Earthquake earthquake : earthquakes) {
                        if (couldBeArrival(event, earthquake, true, true, false)) {
                            map.putIfAbsent(earthquake, event);
                        }
                    }

                    for (Entry<Earthquake, Event> entry : map.entrySet()) {
                        Cluster cluster = entry.getKey().getCluster();
                        Event event2 = entry.getValue();
                        if (!cluster.containsStation(event2.getAnalysis().getStation())) {
                            if (cluster.getAssignedEvents().putIfAbsent(station, event2) == null) {
                                event2.assignedCluster = cluster;
                            }
                        }
                    }
                }
            }
        }

    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean couldBeSArrival(Event event, Earthquake earthquake){
        if (!event.isValid() || earthquake == null) {
            return false;
        }
        long actualTravel = event.getpWave() - earthquake.getOrigin();

        double distGC = GeoUtils.greatCircleDistance(earthquake.getLat(), earthquake.getLon(),
                event.getLatFromStation(), event.getLonFromStation());
        double angle = TauPTravelTimeCalculator.toAngle(distGC);
        double expectedTravelSRaw = TauPTravelTimeCalculator.getSWaveTravelTime(earthquake.getDepth(),
                angle);


        double expectedIntensity = IntensityTable.getMaxIntensity(earthquake.getMag(), GeoUtils.gcdToGeo(distGC));
        if (expectedIntensity < 3.0) {
            return false;
        }

        if (expectedTravelSRaw != TauPTravelTimeCalculator.NO_ARRIVAL) {
            // 985 because GQ has high tendency to detect S waves earlier
            long expectedTravel = (long) ((expectedTravelSRaw + EarthquakeAnalysis.getElevationCorrection(event.getElevationFromStation()) * 1.5) * 985);
            if (Math.abs(expectedTravel - actualTravel) < 1000 + expectedTravel * 0.01) {
                return true;
            }
        }

        return false;
    }

    public static boolean couldBeArrival(PickedEvent pickedEvent, PreliminaryHypocenter bestHypocenter,
                                         boolean considerIntensity, boolean increasingPWindow, boolean pWaveOnly) {
        if (pickedEvent == null || bestHypocenter == null) {
            return false;
        }

        if(considerIntensity){
            throw new IllegalArgumentException("Preliminary Hypocenter doesn't have magnitude and cannot be assessed using intensity.");
        }

        return couldBeArrival(pickedEvent.lat(), pickedEvent.lon(), pickedEvent.elevation(), pickedEvent.pWave(),
                bestHypocenter.lat, bestHypocenter.lon, bestHypocenter.depth, bestHypocenter.origin, 0,
                false, increasingPWindow, pWaveOnly);
    }

    public static boolean couldBeArrival(Event event, Earthquake earthquake,
                                         boolean considerIntensity, boolean increasingPWindow, boolean pWaveOnly) {
        if (event == null || !event.isValid() || event.isSWave() || earthquake == null) {
            return false;
        }

        return couldBeArrival(event.getLatFromStation(), event.getLonFromStation(), event.getElevationFromStation(), event.getpWave(),
                earthquake.getLat(), earthquake.getLon(), earthquake.getDepth(), earthquake.getOrigin(), earthquake.getMag(),
                considerIntensity, increasingPWindow, pWaveOnly);
    }

    public static boolean couldBeArrival(PickedEvent event, Hypocenter earthquake,
                                         boolean considerIntensity, boolean increasingPWindow, boolean pWaveOnly) {
        if (event == null || earthquake == null) {
            return false;
        }

        return couldBeArrival(event.lat(), event.lon(), event.elevation(), event.pWave(),
                earthquake.lat, earthquake.lon, earthquake.depth, earthquake.origin, earthquake.magnitude,
                considerIntensity, increasingPWindow, pWaveOnly);
    }

    @SuppressWarnings("RedundantIfStatement")
    public static boolean couldBeArrival(double eventLat, double eventLon, double eventAlt, long pWave,
                                         double quakeLat, double quakeLon, double quakeDepth, long quakeOrigin, double quakeMag,
                                         boolean considerIntensity, boolean increasingPWindow, boolean pWaveOnly){
        long actualTravel = pWave - quakeOrigin;

        double distGC = GeoUtils.greatCircleDistance(quakeLat, quakeLon,
                eventLat, eventLon);
        double angle = TauPTravelTimeCalculator.toAngle(distGC);
        double expectedTravelPRaw = TauPTravelTimeCalculator.getPWaveTravelTime(quakeDepth,
                angle);

        if(considerIntensity) {
            double expectedIntensity = IntensityTable.getMaxIntensity(quakeMag, GeoUtils.gcdToGeo(distGC));
            if (expectedIntensity < 3.0) {
                return false;
            }
        }

        if (expectedTravelPRaw != TauPTravelTimeCalculator.NO_ARRIVAL) {
            long expectedTravel = (long) ((expectedTravelPRaw + EarthquakeAnalysis.getElevationCorrection(eventAlt)) * 1000);
            if (Math.abs(expectedTravel - actualTravel) < (increasingPWindow ? Math.max(5000, 1000 + expectedTravel * 0.01) : Settings.pWaveInaccuracyThreshold)) {
                return true;
            }
        }

        if(pWaveOnly){
            return false;
        }

        double expectedTravelPKPRaw = TauPTravelTimeCalculator.getPKPWaveTravelTime(quakeDepth,
                angle);

        if (expectedTravelPKPRaw != TauPTravelTimeCalculator.NO_ARRIVAL) {
            long expectedTravel = (long) ((expectedTravelPKPRaw + EarthquakeAnalysis.getElevationCorrection(eventAlt)) * 1000);
            if (Math.abs(expectedTravel - actualTravel) < (Math.max(6000, expectedTravel * 0.005))) {
                return true;
            }
        }

        double expectedTravelPKIKPRaw = TauPTravelTimeCalculator.getPKIKPWaveTravelTime(quakeDepth,
                angle);

        if (expectedTravelPKIKPRaw != TauPTravelTimeCalculator.NO_ARRIVAL && angle > 100) {
            long expectedTravel = (long) ((expectedTravelPKIKPRaw + EarthquakeAnalysis.getElevationCorrection(eventAlt)) * 1000);
            if (Math.abs(expectedTravel - actualTravel) < Math.max(6000, expectedTravel * 0.005)) {
                return true;
            }
        }

        return false;
    }

    private void expandExistingClusters() {
        for (Cluster c : clusters) {
            expandCluster(c);
        }
    }

    private void expandCluster(Cluster cluster) {
        if (cluster.getEarthquake() != null && cluster.getPreviousHypocenter() != null) {
            if(cluster.getPreviousHypocenter().correctEvents > 7) {
                expandPWaves(cluster);
            }

            if(cluster.getPreviousHypocenter().correctEvents > 6) {
                markPossibleSWaves(cluster);
            }
        }

        ArrayList<Event> list = new ArrayList<>(cluster.getAssignedEvents().values());
        while (!list.isEmpty()) {
            ArrayList<Event> newEvents = new ArrayList<>();
            mainLoop:
            for (Event e : list) {
                for (NearbyStationDistanceInfo info : e.getAnalysis().getStation().getNearbyStations()) {
                    if (!cluster.containsStation(info.station()) && !_contains(newEvents, info.station())) {
                        double dist = info.dist();
                        for (Event ev : info.station().getAnalysis().getDetectedEvents()) {
                            if (potentialArrival(ev, e, dist)) {
                                newEvents.add(ev);
                                continue mainLoop;
                            }
                        }
                    }
                }
            }

            for (Event event : newEvents) {
                if (cluster.getAssignedEvents().putIfAbsent(event.getAnalysis().getStation(), event) == null) {
                    event.assignedCluster = cluster;
                }
            }

            list.clear();
            list.addAll(newEvents);
        }
    }

    private void markPossibleSWaves(Cluster cluster) {
        for (AbstractStation station : stations) {
            for (Event event : station.getAnalysis().getDetectedEvents()) {
                if (event.isValid() && couldBeSArrival(event, cluster.getEarthquake())) {
                    event.setAsSWave(true);
                }
            }
        }
    }

    private void expandPWaves(Cluster cluster) {
        mainLoop:
        for (AbstractStation station : stations) {
            for (Event event : station.getAnalysis().getDetectedEvents()) {
                if (event.isValid() && !cluster.containsStation(station) && couldBeArrival(event, cluster.getEarthquake(), true, true, false)) {
                    if (cluster.getAssignedEvents().putIfAbsent(station, event) == null) {
                        event.assignedCluster = cluster;
                    }
                    continue mainLoop;
                }
            }
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean potentialArrival(Event ev, Event e, double dist) {
        if (e.isValid() && ev.isValid() && ev.getpWave() > 0 && ev.assignedCluster == null) {
            long earliestPossibleTimeOfThatEvent = e.getpWave() - (long) ((dist * 1000.0) / 5.0)
                    - 2500;
            long latestPossibleTimeOfThatEvent = e.getpWave() + (long) ((dist * 1000.0) / 5.0)
                    + 2500;
            if (ev.getpWave() >= earliestPossibleTimeOfThatEvent
                    && ev.getpWave() <= latestPossibleTimeOfThatEvent) {
                return true;
            }
        }

        return false;
    }

    private boolean _contains(ArrayList<Event> newEvents, AbstractStation station) {
        for (Event e : newEvents) {
            if (e.getAnalysis().getStation().getId() == station.getId()) {
                return true;
            }
        }
        return false;
    }

    private void createNewClusters() {
        for (AbstractStation station : stations) {
            for (Event event : station.getAnalysis().getDetectedEvents()) {
                if (event.isValid() && event.getpWave() > 0 && event.assignedCluster == null) {
                    // so we have eligible event
                    ArrayList<Event> validEvents = new ArrayList<>();
                    closestLoop:
                    for (NearbyStationDistanceInfo info : station.getNearbyStations()) {
                        AbstractStation close = info.station();
                        double dist = info.dist();
                        for (Event e : close.getAnalysis().getDetectedEvents()) {
                            if (e.isValid() && e.getpWave() > 0 && e.assignedCluster == null) {
                                long earliestPossibleTimeOfThatEvent = event.getpWave()
                                        - (long) ((dist * 1000.0) / 5.0) - 2500;
                                long latestPossibleTimeOfThatEvent = event.getpWave()
                                        + (long) ((dist * 1000.0) / 5.0) + 2500;
                                if (e.getpWave() >= earliestPossibleTimeOfThatEvent
                                        && e.getpWave() <= latestPossibleTimeOfThatEvent) {
                                    validEvents.add(e);
                                    continue closestLoop;
                                }
                            }
                        }
                    }
                    // so no we have a list of all nearby events that could be earthquake
                    if (validEvents.size() >= MIN_CLUSTER_SIZE) {
                        validEvents.add(event);
                        expandCluster(createCluster(validEvents));
                    }
                }
            }
        }

    }

    private void updateClusters() {
        Iterator<Cluster> it = clusters.iterator();
        List<Cluster> toBeRemoved = new ArrayList<>();
        while (it.hasNext()) {
            Cluster cluster = it.next();
            int numberOfActiveEvents = 0;
            int minimum = (int) Math.max(2, cluster.getAssignedEvents().size() * 0.12);
            for (Iterator<Event> iterator = cluster.getAssignedEvents().values().iterator(); iterator.hasNext(); ) {
                Event event = iterator.next();
                if (!event.isValid()) {
                    event.assignedCluster = null;
                    iterator.remove();
                } else if (!event.hasEnded()) {
                    numberOfActiveEvents++;
                }
            }
            if (cluster.getAssignedEvents().size() < MIN_CLUSTER_SIZE || (numberOfActiveEvents < minimum && System.currentTimeMillis() - cluster.getLastUpdate() > 2 * 60 * 1000)) {
                Logger.debug("Cluster #" + cluster.getId() + " died");
                toBeRemoved.add(cluster);
            } else {
                cluster.tick();
            }
        }

        clusters.removeAll(toBeRemoved);
    }

    private Cluster createCluster(ArrayList<Event> validEvents) {
        Cluster cluster = new Cluster(nextClusterId.getAndIncrement());
        for (Event ev : validEvents) {
            if (cluster.getAssignedEvents().putIfAbsent(ev.getAnalysis().getStation(), ev) == null) {
                ev.assignedCluster = cluster;
                cluster.addEvent();
            }
        }

        cluster.calculateRoot();

        Logger.debug("New Cluster #" + cluster.getId() + " Has been created. It contains "
                + cluster.getAssignedEvents().size() + " events");
        clusters.add(cluster);

        if(GlobalQuake.instance != null){
            GlobalQuake.instance.getEventHandler().fireEvent(new ClusterCreateEvent(cluster));
        }

        return cluster;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

}
