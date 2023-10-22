package gqserver.events.specific;

import gqserver.core.earthquake.data.Earthquake;
import gqserver.core.earthquake.data.Hypocenter;
import gqserver.events.GlobalQuakeEventListener;

public record QuakeUpdateEvent(Earthquake earthquake, Hypocenter previousHypocenter) implements GlobalQuakeEvent {

    @Override
    @SuppressWarnings("unused")
    public Earthquake earthquake() {
        return earthquake;
    }

    @Override
    @SuppressWarnings("unused")
    public Hypocenter previousHypocenter() {
        return previousHypocenter;
    }

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onQuakeUpdate(this);
    }
}
