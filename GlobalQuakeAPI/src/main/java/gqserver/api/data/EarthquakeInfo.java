package gqserver.api.data;

import java.io.Serializable;
import java.util.UUID;

public record EarthquakeInfo(UUID uuid, int revisionID) implements Serializable {

    public static final int REMOVED = -1;
}
