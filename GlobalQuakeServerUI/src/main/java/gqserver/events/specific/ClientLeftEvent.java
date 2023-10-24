package gqserver.events.specific;

import globalquake.core.events.specific.GlobalQuakeEvent;
import gqserver.api.ServerClient;
import globalquake.core.events.GlobalQuakeEventListener;
import gqserver.events.GlobalQuakeServerEvent;
import gqserver.events.GlobalQuakeServerEventListener;

public record ClientLeftEvent(ServerClient client) implements GlobalQuakeServerEvent {

    @Override
    public void run(GlobalQuakeServerEventListener eventListener) {
        eventListener.onClientLeave(this);
    }
}
