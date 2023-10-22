package gqserver.events.specific;

import gqserver.core.alert.Warnable;
import gqserver.core.alert.Warning;
import gqserver.events.GlobalQuakeEventListener;

public record AlertIssuedEvent(Warnable warnable, Warning warning) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onWarningIssued(this);
    }
}
