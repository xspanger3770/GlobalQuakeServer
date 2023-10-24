package gqserver.server;

import gqserver.api.Packet;
import gqserver.api.ServerClient;
import gqserver.api.data.EarthquakeInfo;
import gqserver.api.data.HypocenterData;
import gqserver.api.packets.earthquake.EarthquakeCheckPacket;
import gqserver.api.packets.earthquake.EarthquakeRequestPacket;
import gqserver.api.packets.earthquake.EarthquakesRequestPacket;
import gqserver.api.packets.earthquake.HypocenterDataPacket;
import gqserver.core.GlobalQuakeServer;
import gqserver.core.earthquake.data.Earthquake;
import gqserver.events.GlobalQuakeEventAdapter;
import gqserver.events.specific.QuakeCreateEvent;
import gqserver.events.specific.QuakeRemoveEvent;
import gqserver.events.specific.QuakeUpdateEvent;
import org.tinylog.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DataService {

    private final ReadWriteLock quakesRWLock = new ReentrantReadWriteLock();

    private final Lock quakesReadLock = quakesRWLock.readLock();
    private final Lock quakesWriteLock = quakesRWLock.writeLock();

    private final List<EarthquakeInfo> currentEarthquakes;

    public DataService() {
        currentEarthquakes = new ArrayList<>();

        GlobalQuakeServer.instance.getEventHandler().registerEventListener(new GlobalQuakeEventAdapter(){
            @Override
            public void onQuakeCreate(QuakeCreateEvent event) {
                Earthquake earthquake = event.earthquake();

                quakesWriteLock.lock();
                try{
                    currentEarthquakes.add(new EarthquakeInfo(earthquake.getUuid(), earthquake.getRevisionID()));
                } finally {
                    quakesWriteLock.unlock();
                }

                broadcast(getEarthquakeReceivingClients(), createQuakePacket(earthquake));
            }

            @Override
            public void onQuakeRemove(QuakeRemoveEvent event) {
                quakesWriteLock.lock();
                try{
                    Earthquake earthquake = event.earthquake();
                    for (Iterator<EarthquakeInfo> iterator = currentEarthquakes.iterator(); iterator.hasNext(); ) {
                        EarthquakeInfo info = iterator.next();
                        if (info.uuid().equals(earthquake.getUuid())) {
                            iterator.remove();
                            break;
                        }
                    }
                } finally {
                    quakesWriteLock.unlock();
                }

                broadcast(getEarthquakeReceivingClients(), new EarthquakeCheckPacket(new EarthquakeInfo(event.earthquake().getUuid(), EarthquakeInfo.REMOVED)));
            }

            @Override
            public void onQuakeUpdate(QuakeUpdateEvent event) {
                quakesWriteLock.lock();
                Earthquake earthquake = event.earthquake();

                try{
                    for (Iterator<EarthquakeInfo> iterator = currentEarthquakes.iterator(); iterator.hasNext(); ) {
                        EarthquakeInfo info = iterator.next();
                        if (info.uuid().equals(earthquake.getUuid())) {
                            iterator.remove();
                            break;
                        }
                    }

                    currentEarthquakes.add(new EarthquakeInfo(earthquake.getUuid(), earthquake.getRevisionID()));
                } finally {
                    quakesWriteLock.unlock();
                }

                broadcast(getEarthquakeReceivingClients(), createQuakePacket(earthquake));
            }
        });

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::checkQuakes, 0, 1, TimeUnit.MINUTES);
    }

    private void checkQuakes() {
        quakesWriteLock.lock();
        try{
            currentEarthquakes.removeIf(info -> !existsQuake(info.uuid()));
        } finally {
            quakesWriteLock.unlock();
        }
    }

    private boolean existsQuake(UUID uuid) {
        return GlobalQuakeServer.instance.getEarthquakeAnalysis().getEarthquakes().stream()
                .anyMatch(earthquake -> earthquake.getUuid().equals(uuid));
    }

    private Packet createQuakePacket(Earthquake earthquake) {
        return new HypocenterDataPacket(new HypocenterData(
               earthquake.getUuid(),earthquake.getRevisionID(), earthquake.getLat(), earthquake.getLon(),
                earthquake.getDepth(),earthquake.getOrigin(), earthquake.getMag())
        );
    }

    private void broadcast(List<ServerClient> clients, Packet packet) {
        clients.forEach(client -> {
            try {
                client.sendPacket(packet);
            } catch (IOException e) {
                Logger.error(e);
            }
        });
    }

    private List<ServerClient> getEarthquakeReceivingClients(){
        return getClients().stream().filter(serverClient -> serverClient.getClientConfig().earthquakeData()).toList();
    }

    private List<ServerClient> getClients() {
        return GlobalQuakeServer.instance.getServerSocket().getClients();
    }

    public void processPacket(ServerClient client, Packet packet) {
        try {
            if (packet instanceof EarthquakesRequestPacket) {
                processEarthquakesRequest(client);
            } else if (packet instanceof EarthquakeRequestPacket earthquakeRequestPacket) {
                processEarthquakeRequest(client, earthquakeRequestPacket);
            }
        }catch(IOException e){
            Logger.error(e);
        }
    }

    private void processEarthquakeRequest(ServerClient client, EarthquakeRequestPacket earthquakeRequestPacket) throws IOException {
        for(Earthquake earthquake : GlobalQuakeServer.instance.getEarthquakeAnalysis().getEarthquakes()){
            if(earthquake.getUuid().equals(earthquakeRequestPacket.getUuid())){
                client.sendPacket(createQuakePacket(earthquake));
                return;
            }
        }
    }

    private void processEarthquakesRequest(ServerClient client) throws IOException {
        quakesReadLock.lock();
        try {
            for (EarthquakeInfo info : currentEarthquakes) {
                client.sendPacket(new EarthquakeCheckPacket(info));
            }
        } finally {
            quakesReadLock.unlock();
        }
    }
}
