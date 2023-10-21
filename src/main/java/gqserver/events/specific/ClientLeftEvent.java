package gqserver.events.specific;

import gqserver.api.ServerClient;
import gqserver.events.GlobalQuakeEventListener;

public record ClientLeftEvent(ServerClient client) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onClientLeave(this);
    }
}
