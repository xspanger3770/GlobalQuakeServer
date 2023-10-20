package gqserver.events;

import gqserver.events.specific.*;

public interface GlobalQuakeEventListener {

    void onClusterCreate(ClusterCreateEvent event);

    void onQuakeCreate(QuakeCreateEvent event);

    @SuppressWarnings("unused")
    void onQuakeUpdate(QuakeUpdateEvent event);

    void onWarningIssued(AlertIssuedEvent event);

    void onQuakeRemove(QuakeRemoveEvent quakeRemoveEvent);

    @SuppressWarnings("unused")
    void onShakemapCreated(ShakeMapCreatedEvent shakeMapCreatedEvent);

}
