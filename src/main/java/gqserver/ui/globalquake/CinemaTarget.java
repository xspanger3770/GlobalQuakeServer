package gqserver.ui.globalquake;

import gqserver.core.alert.Warnable;

public record CinemaTarget(double lat, double lon, double zoom, double priority, Warnable original) {
}
