package globalquake.core.events.specific;

import globalquake.core.earthquake.data.Earthquake;
import globalquake.core.earthquake.data.Hypocenter;
import globalquake.core.events.GlobalQuakeEventListener;

public record QuakeArchiveEvent(Earthquake earthquake) implements GlobalQuakeEvent {

    @Override
    public void run(GlobalQuakeEventListener eventListener) {
        eventListener.onQuakeArchive(this);
    }
}
