package gqserver.api.data;

import java.io.Serializable;

public record ServerClientConfig(boolean earthquakeData, boolean stationData) implements Serializable {
}
