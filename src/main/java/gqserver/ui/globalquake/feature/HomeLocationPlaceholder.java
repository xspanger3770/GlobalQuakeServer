package gqserver.ui.globalquake.feature;

import gqserver.ui.settings.Settings;

class HomeLocationPlaceholder implements LocationPlaceholder {
    public double getLat() {
        return Settings.homeLat;
    }

    public double getLon() {
        return Settings.homeLon;
    }
}
