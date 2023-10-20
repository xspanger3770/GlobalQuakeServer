package globalquake.main;

import globalquake.core.GlobalQuake;
import globalquake.database.StationDatabaseManager;
import globalquake.database.StationSource;
import globalquake.exception.ApplicationErrorHandler;
import globalquake.exception.RuntimeApplicationException;
import globalquake.exception.FatalIOException;
import globalquake.geo.taup.TauPTravelTimeCalculator;
import globalquake.intensity.IntensityTable;
import globalquake.intensity.ShakeMap;
import globalquake.regions.Regions;
import globalquake.sounds.Sounds;
import globalquake.training.EarthquakeAnalysisTraining;
import globalquake.ui.database.DatabaseMonitorFrame;
import globalquake.ui.settings.Settings;
import globalquake.utils.Scale;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Objects;
import java.util.stream.Collectors;

public class Main {

    private static ApplicationErrorHandler errorHandler;
    public static final String version = "0.9.9_pre5";
    public static final String fullName = "GlobalQuake " + version;
    public static final File MAIN_FOLDER = new File("./GlobalQuake/");
    private static DatabaseMonitorFrame databaseMonitorFrame;
    private static StationDatabaseManager databaseManager;

    public static final Image LOGO = new ImageIcon(Objects.requireNonNull(ClassLoader.getSystemClassLoader().getResource("logo/logo.png"))).getImage();

    private static void startDatabaseManager() throws FatalIOException {
        databaseManager = new StationDatabaseManager();
        databaseManager.load();
        databaseMonitorFrame = new DatabaseMonitorFrame(databaseManager, Main::launchGlobalQuake);
        databaseMonitorFrame.setVisible(true);
    }

    public static void main(String[] args) {
        initErrorHandler();

        try {
            if (!MAIN_FOLDER.exists()) {
                if (!MAIN_FOLDER.mkdirs()) {
                    getErrorHandler().handleException(new FatalIOException("Unable to create main directory!", null));
                }
            }

            startDatabaseManager();

            new Thread("Init Thread") {
                @Override
                public void run() {
                    try {
                        initAll();
                    } catch (Exception e) {
                        getErrorHandler().handleException(e);
                    }
                }
            }.start();
        } catch (Exception e) {
            getErrorHandler().handleException(e);
        }
    }

    private static final double PHASES = 9.0;
    private static int phase = 0;

    private static void initAll() throws Exception {
        databaseMonitorFrame.getMainProgressBar().setString("Loading regions...");
        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
        Regions.init();
        databaseMonitorFrame.getMainProgressBar().setString("Loading scales...");
        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
        Scale.load();
        databaseMonitorFrame.getMainProgressBar().setString("Loading shakemap...");
        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
        ShakeMap.init();
        databaseMonitorFrame.getMainProgressBar().setString("Loading sounds...");
        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
        try{
            //Sound may fail to load for a variety of reasons. If it does, this method disables sound.
            Sounds.load();
        } catch (Exception e){
            RuntimeApplicationException error = new RuntimeApplicationException("Failed to load sounds. Sound will be disabled", e);
            getErrorHandler().handleWarning(error);
        }
        databaseMonitorFrame.getMainProgressBar().setString("Filling up intensity table...");
        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
        IntensityTable.init();
        databaseMonitorFrame.getMainProgressBar().setString("Loading travel table...");
        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
        TauPTravelTimeCalculator.init();
        databaseMonitorFrame.getMainProgressBar().setString("Calibrating...");
        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
        if(Settings.recalibrateOnLaunch) {
            EarthquakeAnalysisTraining.calibrateResolution(databaseMonitorFrame.getMainProgressBar(), null);
        }
        databaseMonitorFrame.getMainProgressBar().setString("Updating Station Sources...");
        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
        databaseManager.runUpdate(databaseManager.getStationDatabase().getStationSources().stream()
                        .filter(StationSource::isOutdated).collect(Collectors.toList()),
                () -> {
                    databaseMonitorFrame.getMainProgressBar().setString("Checking Seedlink Networks...");
                    databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
                    databaseManager.runAvailabilityCheck(databaseManager.getStationDatabase().getSeedlinkNetworks(), () -> {
                        databaseMonitorFrame.getMainProgressBar().setString("Saving...");
                        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));

                        try {
                            databaseManager.save();
                        } catch (FatalIOException e) {
                            getErrorHandler().handleException(new RuntimeException(e));
                        }
                        databaseMonitorFrame.initDone();

                        databaseMonitorFrame.getMainProgressBar().setString("Done");
                        databaseMonitorFrame.getMainProgressBar().setValue((int) ((phase++ / PHASES) * 100.0));
                    });
                });
    }

    public static void launchGlobalQuake() {
        new GlobalQuake(databaseManager).createFrame().runSeedlinkReader().startRuntime();
    }

    public static ApplicationErrorHandler getErrorHandler() {
        if(errorHandler == null) {
            errorHandler = new ApplicationErrorHandler(null);
        }
        return errorHandler;
    }

    public static void initErrorHandler() {
        Thread.setDefaultUncaughtExceptionHandler(getErrorHandler());
    }
}
