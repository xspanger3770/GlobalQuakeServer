package gqserver.events.specific;

import gqserver.events.GlobalQuakeEventListener;
import gqserver.ui.globalquake.CinemaTarget;

public record CinemaEvent(CinemaTarget cinemaTarget) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onCinemaModeTargetSwitch(this);
    }

    @Override
    public CinemaTarget cinemaTarget() {
        return cinemaTarget;
    }
}
