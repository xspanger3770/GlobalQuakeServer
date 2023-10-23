package gqserver.core;

import gqserver.core.station.AbstractStation;
import gqserver.main.Main;
import gqserver.utils.NamedThreadFactory;
import org.tinylog.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class GlobalQuakeRuntime {

    private long lastSecond;
    private long lastAnalysis;
    private long lastGC;
    private long clusterAnalysisT;
    private long lastQuakesT;
    private ScheduledExecutorService execAnalysis;
    private ScheduledExecutorService exec1Sec;
    private ScheduledExecutorService execClusters;
    private ScheduledExecutorService execQuake;

    public void runThreads() {
        execAnalysis = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("Station Analysis Thread"));
        exec1Sec = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("1-Second Loop Thread"));
        execClusters = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("Cluster Analysis Thread"));
        execQuake = Executors
                .newSingleThreadScheduledExecutor(new NamedThreadFactory("Hypocenter Location Thread"));

        execAnalysis.scheduleAtFixedRate(() -> {
            try {
                long a = System.currentTimeMillis();
                GlobalQuakeServer.instance.getStationManager().getStations().parallelStream().forEach(AbstractStation::analyse);
                lastAnalysis = System.currentTimeMillis() - a;
            } catch (Exception e) {
                Logger.error("Exception occurred in station analysis");
                Main.getErrorHandler().handleException(e);
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        exec1Sec.scheduleAtFixedRate(() -> {
            try {
                long a = System.currentTimeMillis();
                GlobalQuakeServer.instance.getStationManager().getStations().parallelStream().forEach(station -> station.second(a));
                if (GlobalQuakeServer.instance.getEarthquakeAnalysis() != null) {
                    GlobalQuakeServer.instance.getEarthquakeAnalysis().second();
                }
                lastSecond = System.currentTimeMillis() - a;
            } catch (Exception e) {
                Logger.error("Exception occurred in 1-second loop");
                Main.getErrorHandler().handleException(e);
            }
        }, 0, 1, TimeUnit.SECONDS);

        execClusters.scheduleAtFixedRate(() -> {
            try {
                long a = System.currentTimeMillis();
                GlobalQuakeServer.instance.getClusterAnalysis().run();
                clusterAnalysisT = System.currentTimeMillis() - a;
            } catch (Exception e) {
                Logger.error("Exception occured in cluster analysis loop");
                Main.getErrorHandler().handleException(e);
            }
        }, 0, 500, TimeUnit.MILLISECONDS);

        execQuake.scheduleAtFixedRate(() -> {
            try {
                long a = System.currentTimeMillis();
                GlobalQuakeServer.instance.getEarthquakeAnalysis().run();
                lastQuakesT = System.currentTimeMillis() - a;
            } catch (Exception e) {
                Logger.error("Exception occured in hypocenter location loop");
                Main.getErrorHandler().handleException(e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void stop() {
        execAnalysis.shutdownNow();
        execQuake.shutdownNow();
        execClusters.shutdownNow();
        exec1Sec.shutdownNow();

        try {
            execAnalysis.awaitTermination(10, TimeUnit.SECONDS);
            execQuake.awaitTermination(10, TimeUnit.SECONDS);
            execClusters.awaitTermination(10, TimeUnit.SECONDS);
            exec1Sec.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Logger.error(e);
        }
    }
}
