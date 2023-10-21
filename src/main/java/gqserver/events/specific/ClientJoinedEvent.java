package gqserver.events.specific;

import gqserver.api.ServerClient;
import gqserver.events.GlobalQuakeEventListener;
import gqserver.server.SocketStatus;

public record ClientJoinedEvent(ServerClient client) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onClientJoin(this);
    }
}
