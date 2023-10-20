package gqserver.events.specific;

import gqserver.core.alert.Warnable;
import gqserver.core.alert.Warning;
import gqserver.events.GlobalQuakeEventListener;

public record ServerStatusChangedEvent() implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onServerStatusChanged(this);
    }
}
