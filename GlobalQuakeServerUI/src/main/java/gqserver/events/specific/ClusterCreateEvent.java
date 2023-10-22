package gqserver.events.specific;

import gqserver.core.earthquake.data.Cluster;
import gqserver.events.GlobalQuakeEventListener;

public record ClusterCreateEvent(Cluster cluster) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onClusterCreate(this);
    }
}
