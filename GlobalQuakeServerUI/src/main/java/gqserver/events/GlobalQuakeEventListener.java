package gqserver.events;

import gqserver.events.specific.*;

public interface GlobalQuakeEventListener {

    void onClusterCreate(ClusterCreateEvent event);

    void onQuakeCreate(QuakeCreateEvent event);

    @SuppressWarnings("unused")
    void onQuakeUpdate(QuakeUpdateEvent event);

    void onQuakeRemove(QuakeRemoveEvent quakeRemoveEvent);

    void onServerStatusChanged(ServerStatusChangedEvent serverStatusChangedEvent);

    void onClientJoin(ClientJoinedEvent clientJoinedEvent);

    void onClientLeave(ClientLeftEvent clientLeftEvent);
}
