package gqserver.events.specific;

import globalquake.core.events.GlobalQuakeEventListener;
import globalquake.core.events.specific.GlobalQuakeEvent;
import gqserver.events.GlobalQuakeServerEvent;
import gqserver.events.GlobalQuakeServerEventListener;
import gqserver.server.SocketStatus;

public record ServerStatusChangedEvent(SocketStatus status) implements GlobalQuakeServerEvent {

    @Override
    public void run(GlobalQuakeServerEventListener eventListener) {
        eventListener.onServerStatusChanged(this);
    }
}
