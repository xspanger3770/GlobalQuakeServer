package gqserver.api.data;

import java.io.Serializable;
import java.util.UUID;

public record HypocenterData(UUID uuid, int revisionID, double lat, double lon, double depth, long origin, double magnitude) implements Serializable {
}
