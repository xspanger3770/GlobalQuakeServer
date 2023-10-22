package gqserver.events.specific;

import gqserver.events.GlobalQuakeEventListener;

public interface GlobalQuakeEvent {

    void run(GlobalQuakeEventListener eventListener);
}
