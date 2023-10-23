package gqserver.events;

import gqserver.events.specific.*;

public class GlobalQuakeEventAdapter implements GlobalQuakeEventListener{

    @Override
    public void onClusterCreate(ClusterCreateEvent event) {

    }

    @Override
    public void onQuakeCreate(QuakeCreateEvent event) {

    }

    @SuppressWarnings("unused")
    @Override
    public void onQuakeUpdate(QuakeUpdateEvent event) {

    }

    @Override
    public void onQuakeRemove(QuakeRemoveEvent quakeRemoveEvent) {

    }

    @Override
    public void onServerStatusChanged(ServerStatusChangedEvent serverStatusChangedEvent) {

    }

    @Override
    public void onClientJoin(ClientJoinedEvent clientJoinedEvent) {

    }

    @Override
    public void onClientLeave(ClientLeftEvent clientLeftEvent) {

    }
}
