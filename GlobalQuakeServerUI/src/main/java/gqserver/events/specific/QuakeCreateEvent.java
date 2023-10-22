package gqserver.events.specific;

import gqserver.core.earthquake.data.Earthquake;
import gqserver.events.GlobalQuakeEventListener;

public record QuakeCreateEvent(Earthquake earthquake) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onQuakeCreate(this);
    }
}
