package gqserver.api.data;

import java.io.Serializable;
import java.util.UUID;

public record EarthquakeInfo(UUID uuid, int revisionID) implements Serializable {

}
