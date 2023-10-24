package globalquake.core.report;

import globalquake.core.GlobalQuake;
import globalquake.core.regions.Regions;
import globalquake.core.station.AbstractStation;
import globalquake.core.earthquake.data.Earthquake;
import globalquake.core.analysis.Event;
import globalquake.utils.GeoUtils;
import globalquake.utils.Scale;
import org.geojson.LngLatAlt;
import org.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class EarthquakeReporter {
	public static final File ANALYSIS_FOLDER = new File(GlobalQuake.mainFolder, "/events/");
	private static final DateTimeFormatter fileFormat = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss").withZone(ZoneId.systemDefault());
	private static double centerLat = 49.7;
	private static double centerLon = 15.65;
	private static double scroll = 8;
	private static final int width = 600;
	private static final int height = 600;

	private static final Color oceanC = new Color(7, 37, 48);
	private static final Color landC = new Color(15, 47, 68);
	private static final Color borderC = new Color(153, 153, 153);

	public static void report(Earthquake earthquake) {
		File folder = new File(ANALYSIS_FOLDER, String.format("M%2.2f_%s_%s", earthquake.getMag(),
				earthquake.getRegion().replace(' ', '_'), fileFormat.format(Instant.ofEpochMilli(earthquake.getOrigin())) + "/"));
		if (!folder.exists()) {
			if(!folder.mkdirs()){
				return;
			}
		}

		for (Event e : earthquake.getCluster().getAssignedEvents().values()) {
			AbstractStation station = e.getAnalysis().getStation();
			e.report = new StationReport(station.getNetworkCode(), station.getStationCode(),
					station.getChannelName(), station.getLocationCode(), station.getLatitude(), station.getLongitude(),
					station.getAlt());
		}

		drawMap(folder, earthquake);
		drawIntensities(folder, earthquake);
	}

	private static void calculatePos(Earthquake earthquake) {
		centerLat = earthquake.getLat();
		centerLon = earthquake.getLon();
		scroll = 2;
	}

	private static void drawIntensities(File folder, Earthquake earthquake) {
		int w = 800;
		int h = 600;
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = img.createGraphics();

		ArrayList<DistanceIntensityRecord> recs = new ArrayList<>();
		for (Event event : earthquake.getCluster().getAssignedEvents().values()) {
			double lat = event.report.lat();
			double lon = event.report.lon();
			double distGE = GeoUtils.geologicalDistance(earthquake.getLat(), earthquake.getLon(),
					-earthquake.getDepth(), lat, lon, event.report.alt() / 1000.0);
			recs.add(new DistanceIntensityRecord(0, distGE, event.maxRatio));
		}

		IntensityGraphs.drawGraph(g, w, h, recs);

		g.dispose();
		try {
			ImageIO.write(img, "PNG", new File(folder, "intensities.png"));
		} catch (IOException e) {
			Logger.error(e);
		}
	}

	private static void drawMap(File folder, Earthquake earthquake) {
		calculatePos(earthquake);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = img.createGraphics();
		g.setColor(oceanC);
		g.fillRect(0, 0, width, height);

        ArrayList<org.geojson.Polygon> pols = scroll < 0.6 ? Regions.raw_polygonsUHD
                : scroll < 4.8 ? Regions.raw_polygonsHD : Regions.raw_polygonsMD;
        for (org.geojson.Polygon polygon : pols) {
            java.awt.Polygon awt = new java.awt.Polygon();
            boolean add = false;
            for (LngLatAlt pos : polygon.getCoordinates().get(0)) {
                double x = getX(pos.getLongitude());
                double y = getY(pos.getLatitude());

                if (!add && isOnScreen(x, y)) {
                    add = true;
                }
                awt.addPoint((int) x, (int) y);
            }
            if (add) {
                g.setColor(landC);
                g.fill(awt);
                g.setColor(borderC);
                g.draw(awt);
            }
        }

        {
			double x = getX(earthquake.getLon());
			double y = getY(earthquake.getLat());
			double r = 12;
			Line2D.Double line1 = new Line2D.Double(x - r, y - r, x + r, y + r);
			Line2D.Double line2 = new Line2D.Double(x - r, y + r, x + r, y - r);
			g.setColor(Color.white);
			g.setStroke(new BasicStroke(8f));
			g.draw(line1);
			g.draw(line2);
			g.setColor(Color.orange);
			g.setStroke(new BasicStroke(6f));
			g.draw(line1);
			g.draw(line2);
		}

		g.setStroke(new BasicStroke(1f));
		for (Event event : earthquake.getCluster().getAssignedEvents().values()) {
			double x = getX(event.report.lon());
			double y = getY(event.report.lat());
			double r = 12;
			g.setColor(Scale.getColorRatio(event.getMaxRatio()));
			Ellipse2D.Double ell1 = new Ellipse2D.Double(x - r / 2, y - r / 2, r, r);
			g.fill(ell1);
		}

		g.dispose();
		File file = new File(folder, "map.png");
		try {
			ImageIO.write(img, "PNG", file);
		} catch (IOException e) {
			Logger.error(e);
		}
	}

	private static boolean isOnScreen(double x, double y) {
		return x >= 0 && y >= 0 && x < width && y < height;
	}

	private static double getX(double lon) {
		return (lon - centerLon) / (scroll / 100.0) + (width * 0.5);
	}

	private static double getY(double lat) {
		return (centerLat - lat) / (scroll / (300 - 200 * Math.cos(0.5 * Math.toRadians(centerLat + lat))))
				+ (height * 0.5);
	}

	@SuppressWarnings("unused")
	private static double getLat(double y) {
		return centerLat - (y - (height * 0.5)) * (scroll / (300 - 200 * Math.cos(Math.toRadians(centerLat))));

	}

	@SuppressWarnings("unused")
	private static double getLon(double x) {
		return (x - (width * 0.5)) * (scroll / 100.0) + centerLon;
	}

}
