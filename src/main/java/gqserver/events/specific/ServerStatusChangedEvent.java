package gqserver.events.specific;

import gqserver.core.alert.Warnable;
import gqserver.core.alert.Warning;
import gqserver.events.GlobalQuakeEventListener;
import gqserver.server.SocketStatus;

public record ServerStatusChangedEvent(SocketStatus status) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onServerStatusChanged(this);
    }
}
