package gqserver.events.specific;

import gqserver.core.earthquake.data.Earthquake;
import gqserver.events.GlobalQuakeEventListener;

public record QuakeRemoveEvent(Earthquake earthquake) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onQuakeRemove(this);
    }
}
