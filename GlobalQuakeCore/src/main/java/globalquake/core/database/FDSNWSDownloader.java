package globalquake.core.database;

import globalquake.core.exception.FdnwsDownloadException;
import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FDSNWSDownloader {

    private static final DateTimeFormatter format1 = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int TIMEOUT_SECONDS = 120;

    public static final List<Character> SUPPORTED_BANDS = List.of('E', 'S', 'H', 'B');
    public static final List<Character> SUPPORTED_INSTRUMENTS = List.of('H', 'L', 'G', 'M', 'N');

    private static List<String> downloadWadl(StationSource stationSource) throws Exception {
        URL url = new URL("%sapplication.wadl".formatted(stationSource.getUrl()));

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(TIMEOUT_SECONDS * 1000);
        con.setReadTimeout(TIMEOUT_SECONDS * 1000);
        InputStream inp = con.getInputStream();

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inp);
        doc.getDocumentElement().normalize();

        List<String> paramNames = new ArrayList<>();
        NodeList paramNodes = doc.getElementsByTagName("param");
        for (int i = 0; i < paramNodes.getLength(); i++) {
            Node paramNode = paramNodes.item(i);
            if (paramNode.getNodeType() == Node.ELEMENT_NODE) {
                Element paramElement = (Element) paramNode;
                String paramName = paramElement.getAttribute("name");
                paramNames.add(paramName);
            }
        }

        return paramNames;
    }

    public static List<Network> downloadFDSNWS(StationSource stationSource) throws Exception {
        List<Network> result = new ArrayList<>();
        downloadFDSNWS(stationSource, result, -180, 180);
        System.out.printf("%d Networks downloaded.%n", result.size());
        return result;
    }

    public static void downloadFDSNWS(StationSource stationSource, List<Network> result, double minLon, double maxLon) throws Exception {
        List<String> supportedAttributes = downloadWadl(stationSource);
        URL url;
        if(supportedAttributes.contains("endafter")){
            url = new URL("%squery?minlongitude=%s&maxlongitude=%s&level=channel&endafter=%s&format=xml&channel=??Z".formatted(stationSource.getUrl(), minLon, maxLon, format1.format(Instant.now())));
        } else {
            url = new URL("%squery?minlongitude=%s&maxlongitude=%s&level=channel&format=xml&channel=??Z".formatted(stationSource.getUrl(), minLon, maxLon));
        }


        Logger.info("Connecting to " + url);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(TIMEOUT_SECONDS * 1000);
        con.setReadTimeout(TIMEOUT_SECONDS * 1000);

        int response = con.getResponseCode();

        if (response == 413) {
            Logger.debug("413! Splitting...");
            stationSource.getStatus().setString("Splitting...");
            if(maxLon - minLon < 0.1){
                return;
            }

            downloadFDSNWS(stationSource, result, minLon, (minLon + maxLon) / 2.0);
            downloadFDSNWS(stationSource, result, (minLon + maxLon) / 2.0, maxLon);
        } else if(response / 100 == 2) {
            InputStream inp = con.getInputStream();
            downloadFDSNWS(stationSource, result, inp);
        } else {
            throw new FdnwsDownloadException("HTTP Status %d!".formatted(response));
        }
    }

    private static void downloadFDSNWS(StationSource stationSource, List<Network> result, InputStream inp) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        f.setValidating(false);
        final CountInputStream in = new CountInputStream(inp);

        in.setEvent(() ->  stationSource.getStatus().setString("Downloading %dkB".formatted(in.getCount() / 1024)));

        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        // some FDSNWS providers send empty document if no stations found by given parameters
        if(text.isEmpty()){
            return;
        }

        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(text)));

        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        parseNetworks(result, stationSource, root);
    }

    private static void parseNetworks(List<Network> result, StationSource stationSource, Element root) {
        NodeList networks = root.getElementsByTagName("Network");
        for (int i = 0; i < networks.getLength(); i++) {
            String networkCode = obtainAttribute(networks.item(i), "code", "unknown");
            if (networkCode.equalsIgnoreCase("unknown")) {
                Logger.debug("ERR: no network code wtf.");
                continue;
            }
            String networkDescription = obtainElement(networks.item(i), "Description", "");
            parseStations(result, stationSource, networks, i, networkCode, networkDescription);
        }
    }

    private static void parseStations(List<Network> result, StationSource stationSource, NodeList networks, int i, String networkCode, String networkDescription) {
        NodeList stations = ((Element) networks.item(i)).getElementsByTagName("Station");
        for (int j = 0; j < stations.getLength(); j++) {
            Node stationNode = stations.item(j);
            String stationCode = stationNode.getAttributes().getNamedItem("code").getNodeValue();
            String stationSite = ((Element) stationNode).getElementsByTagName("Site").item(0).getTextContent();

            double lat = Double.parseDouble(
                    ((Element) stationNode).getElementsByTagName("Latitude").item(0).getTextContent());
            double lon = Double.parseDouble(
                    ((Element) stationNode).getElementsByTagName("Longitude").item(0).getTextContent());
            double alt = Double.parseDouble(
                    ((Element) stationNode).getElementsByTagName("Elevation").item(0).getTextContent());

            parseChannels(result, stationSource, networkCode, networkDescription, (Element) stationNode, stationCode, stationSite, lat, lon, alt);
        }
    }

    private static void parseChannels(
            List<Network> result, StationSource stationSource, String networkCode, String networkDescription,
            Element stationNode, String stationCode, String stationSite,
            double stationLat, double stationLon, double stationAlt) {
        NodeList channels = stationNode.getElementsByTagName("Channel");
        for (int k = 0; k < channels.getLength(); k++) {
                // Necessary values: lat lon alt sampleRate, Other can fail

            Node channelNode = channels.item(k);
            String channel = channelNode.getAttributes().getNamedItem("code").getNodeValue();
            String locationCode = channelNode.getAttributes().getNamedItem("locationCode")
                    .getNodeValue();
            double lat = Double.parseDouble(
                    ((Element) channelNode).getElementsByTagName("Latitude").item(0).getTextContent());
            double lon = Double.parseDouble(
                    ((Element) channelNode).getElementsByTagName("Longitude").item(0).getTextContent());
            double alt = Double.parseDouble(
                    ((Element) channelNode).getElementsByTagName("Elevation").item(0).getTextContent());

            var item = ((Element) channelNode)
                    .getElementsByTagName("SampleRate").item(0);

            // sample rate is not actually required as it is provided by the seedlink protocol itself
            double sampleRate = -1;
            if(item != null){
                sampleRate = Double.parseDouble(((Element) channelNode)
                        .getElementsByTagName("SampleRate").item(0).getTextContent());
            }

            if(!isSupported(channel)){
                continue;
            }

            addChannel(result, stationSource, networkCode, networkDescription, stationCode, stationSite, channel,
                    locationCode, lat, lon, alt, sampleRate, stationLat, stationLon, stationAlt);
        }
    }

    private static boolean isSupported(String channel) {
        char band = channel.charAt(0);
        char instrument = channel.charAt(1);

        if(!(SUPPORTED_BANDS.contains(band))){
            return false;
        }


        return SUPPORTED_INSTRUMENTS.contains(instrument);
    }

    private static void addChannel(
            List<Network> result, StationSource stationSource, String networkCode, String networkDescription,
            String stationCode, String stationSite, String channelCode, String locationCode,
            double lat, double lon, double alt, double sampleRate,
            double stationLat, double stationLon, double stationAlt) {
        Network network = StationDatabase.getOrCreateNetwork(result, networkCode, networkDescription);
        Station station = StationDatabase.getOrCreateStation(network, stationCode, stationSite, stationLat, stationLon, stationAlt);
        StationDatabase.getOrCreateChannel(station, channelCode, locationCode, lat, lon, alt, sampleRate, stationSource);
    }

    public static String obtainElement(Node item, String name, String defaultValue) {
        try {
            return ((Element) item).getElementsByTagName(name).item(0).getTextContent();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static String obtainAttribute(Node item, String name, String defaultValue) {
        try {
            return item.getAttributes().getNamedItem(name).getNodeValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

}
