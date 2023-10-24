package gqserver.ui;

import globalquake.core.Settings;
import globalquake.core.analysis.AnalysisStatus;
import globalquake.core.analysis.BetterAnalysis;
import globalquake.core.analysis.Event;
import globalquake.core.analysis.Log;
import globalquake.core.earthquake.EarthquakeAnalysis;
import globalquake.core.earthquake.data.Earthquake;
import globalquake.core.geo.taup.TauPTravelTimeCalculator;
import globalquake.core.station.AbstractStation;
import globalquake.utils.GeoUtils;
import gqserver.server.GlobalQuakeServer;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JPanel;

public class StationMonitorPanel extends JPanel {

	private BufferedImage image;
	private final AbstractStation station;

	public StationMonitorPanel(AbstractStation station) {
		this.station = station;
		setLayout(null);
		setPreferredSize(new Dimension(600, 500));
		setSize(getPreferredSize());
		updateImage();
	}

	private static final Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
			new float[] { 3 }, 0);

	public void updateImage() {
		int w = getWidth();
		int h = getHeight();
		BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, w, h);

		g.setColor(Color.black);
		g.setFont(new Font("Calibri", Font.BOLD, 14));
		g.drawString("Raw Data", 4, 14);
		g.drawString("Band Pass %sHz - %sHz".formatted(BetterAnalysis.min_frequency, BetterAnalysis.max_frequency), 4, (int) (h * 0.2 + 14));
		g.drawString("Running Averages", 4, (int) (h * 0.4 + 14));
		g.drawString("Averages Ratio", 4, (int) (h * 0.7 + 14));

		long upperMinute = (long) (Math.ceil(getTime()/ (1000 * 60.0) + 1) * (1000L * 60L));
		for (int deltaSec = 0; deltaSec <= 60 * Settings.logsStoreTimeMinutes + 80; deltaSec += 10) {
			long time = upperMinute - deltaSec * 1000L;
			boolean fullMinute = time % 60000 == 0;
			double x = getX(time);
			g.setColor(!fullMinute ? Color.lightGray : Color.gray);
			g.setStroke(!fullMinute ? dashed : new BasicStroke(2f));
			g.draw(new Line2D.Double(x, 0, x, getHeight()));
		}

		ArrayList<Log> logs;
		synchronized (station.getAnalysis().previousLogsLock) {
			logs = new ArrayList<>(station.getAnalysis().getPreviousLogs());
		}

		if (logs.size() > 1) {
			double maxValue = -Double.MAX_VALUE;
			double minValue = Double.MAX_VALUE;
			double maxFilteredValue = -Double.MAX_VALUE;
			double minFilteredValue = Double.MAX_VALUE;
			double maxAverage = 0;
			double maxRatio = 0;
			for (Log l : logs) {
				int v = l.rawValue();
				if (v > maxValue) {
					maxValue = v;
				}
				if (v < minValue) {
					minValue = v;
				}

				double fv = l.filteredV();
				if (fv > maxFilteredValue) {
					maxFilteredValue = fv;
				}
				if (fv < minFilteredValue) {
					minFilteredValue = fv;
				}
				double shortAvg = l.shortAverage();
				double longAvg = l.longAverage();
				double medAvg = l.mediumAverage();
				double specAvg = l.specialAverage();
				if (shortAvg > maxAverage) {
					maxAverage = shortAvg;
				}
				if (longAvg > maxAverage) {
					maxAverage = longAvg;
				}
				if (medAvg > maxAverage) {
					maxAverage = medAvg;
				}
				if (specAvg > maxAverage) {
					maxAverage = specAvg;
				}

				double ratio = l.getRatio();
				double medRatio = l.getMediumRatio();
				double thirdRatio = l.getThirdRatio();
				double specRatio = l.getSpecialRatio();
				if (ratio > maxRatio) {
					maxRatio = ratio;
				}
				if (medRatio > maxRatio) {
					maxRatio = medRatio;
				}
				if (thirdRatio > maxRatio) {
					maxRatio = thirdRatio;
				}
				if (specRatio > maxRatio) {
					maxRatio = specRatio;
				}
			}

			maxValue += 10.0;
			minValue -= 10.0;

			double fix1 = (maxValue - minValue) * 0.25 * 0.5;
			maxValue += fix1;
			minValue -= fix1;

			double fix2 = (maxFilteredValue - minFilteredValue) * 0.25 * 0.5;
			maxFilteredValue += fix2;
			minFilteredValue -= fix2;

			maxAverage += 10.0;
			maxAverage *= 1.25;

			for (int i = 0; i < logs.size() - 1; i++) {
				Log a = logs.get(i);
				Log b = logs.get(i + 1);

				boolean gap = (a.time() - b.time()) > (1000.0 / station.getAnalysis().getSampleRate()) * 2;
				if (gap) {
					continue;
				}

				double x1 = getX(a.time());
				double x2 = getX(b.time());

				double y1 = 0 + (getHeight() * 0.20) * (maxValue - a.rawValue()) / (maxValue - minValue);
				double y2 = 0 + (getHeight() * 0.20) * (maxValue - b.rawValue()) / (maxValue - minValue);

				double y3 = getHeight() * 0.20 + (getHeight() * 0.20) * (maxFilteredValue - a.filteredV())
						/ (maxFilteredValue - minFilteredValue);
				double y4 = getHeight() * 0.20 + (getHeight() * 0.20) * (maxFilteredValue - b.filteredV())
						/ (maxFilteredValue - minFilteredValue);

				double y5 = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - a.shortAverage()) / (maxAverage);
				double y6 = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - b.shortAverage()) / (maxAverage);

				double y7 = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - a.longAverage()) / (maxAverage);
				double y8 = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - b.longAverage()) / (maxAverage);

				double y9 = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - a.mediumAverage()) / (maxAverage);
				double y10 = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - b.mediumAverage()) / (maxAverage);

				double y9b = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - a.thirdAverage()) / (maxAverage);
				double y10b = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - b.thirdAverage()) / (maxAverage);

				double y9c = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - a.specialAverage()) / (maxAverage);
				double y10c = getHeight() * 0.40
						+ (getHeight() * 0.30) * (maxAverage - b.specialAverage()) / (maxAverage);

				double y11 = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - a.getRatio()) / (maxRatio);
				double y12 = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - b.getRatio()) / (maxRatio);

				double y13 = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - a.getMediumRatio()) / (maxRatio);
				double y14 = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - b.getMediumRatio()) / (maxRatio);

				double y13b = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - a.getThirdRatio()) / (maxRatio);
				double y14b = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - b.getThirdRatio()) / (maxRatio);

				double y13c = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - a.getSpecialRatio()) / (maxRatio);
				double y14c = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - b.getSpecialRatio()) / (maxRatio);

				double yA = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - 1.0) / (maxRatio);

				g.setColor(Color.blue);
				g.setStroke(new BasicStroke(1f));
				g.draw(new Line2D.Double(x1, y1, x2, y2));

				g.setColor(Color.orange);
				g.setStroke(new BasicStroke(1f));
				g.draw(new Line2D.Double(x1, y3, x2, y4));

				g.setColor(Color.orange);
				g.setStroke(new BasicStroke(3f));
				g.draw(new Line2D.Double(x1, y7, x2, y8));

				g.setColor(Color.blue);
				g.setStroke(new BasicStroke(2f));
				g.draw(new Line2D.Double(x1, y9, x2, y10));

				g.setColor(Color.green);
				g.setStroke(new BasicStroke(2f));
				g.draw(new Line2D.Double(x1, y9b, x2, y10b));

				g.setColor(Color.red);
				g.setStroke(new BasicStroke(2f));
				g.draw(new Line2D.Double(x1, y9c, x2, y10c));

				g.setColor(a.status() == AnalysisStatus.IDLE ? Color.black : Color.green);
				g.setStroke(new BasicStroke(1f));
				g.draw(new Line2D.Double(x1, y5, x2, y6));

				g.setColor(Color.blue);
				g.setStroke(new BasicStroke(2f));
				g.draw(new Line2D.Double(x1, y13, x2, y14));

				g.setColor(Color.green);
				g.setStroke(new BasicStroke(2f));
				g.draw(new Line2D.Double(x1, y13b, x2, y14b));

				g.setColor(Color.red);
				g.setStroke(new BasicStroke(2f));
				g.draw(new Line2D.Double(x1, y13c, x2, y14c));

				g.setColor(a.status() == AnalysisStatus.IDLE ? Color.black : Color.green);
				g.setStroke(new BasicStroke(1f));
				g.draw(new Line2D.Double(x1, y11, x2, y12));

				g.setColor(Color.red);
				g.setStroke(new BasicStroke(1f));
				g.draw(new Line2D.Double(x1, yA, x2, yA));

				for (double d : Event.RECALCULATE_P_WAVE_THRESHOLDS) {
					double _y = getHeight() * 0.70 + (getHeight() * 0.30) * (maxRatio - d) / (maxRatio);
					if (_y > getHeight() * 0.70) {
						g.setColor(Color.magenta);
						g.setStroke(new BasicStroke(1f));
						g.draw(new Line2D.Double(x1, _y, x2, _y));
					}
				}

			}
		}

		for (Event e : station.getAnalysis().getDetectedEvents()) {
			if(!e.isValid()){
				continue;
			}
			double x = getX(e.getpWave());
			g.setColor(e.isSWave() ? Color.red : Color.blue);
			g.setStroke(new BasicStroke(2f));
			g.draw(new Line2D.Double(x, 0, x, getHeight()));
		}

		if(GlobalQuakeServer.instance != null) {
			for (Earthquake earthquake : GlobalQuakeServer.instance.getEarthquakeAnalysis().getEarthquakes()) {
				double distGC = GeoUtils.greatCircleDistance(station.getLatitude(), station.getLongitude(), earthquake.getLat(), earthquake.getLon());
				long arrivalP = (long) (earthquake.getOrigin() + 1000 * (TauPTravelTimeCalculator.getPWaveTravelTime(earthquake.getDepth(),
						TauPTravelTimeCalculator.toAngle(distGC)) + EarthquakeAnalysis.getElevationCorrection(station.getAlt())));

				long arrivalS = (long) (earthquake.getOrigin() + 1000 * (TauPTravelTimeCalculator.getSWaveTravelTime(earthquake.getDepth(),
						TauPTravelTimeCalculator.toAngle(distGC)) + EarthquakeAnalysis.getElevationCorrection(station.getAlt())));

				double xP = getX(arrivalP);
				double xS = getX(arrivalS);

				g.setColor(Color.magenta);
				g.setStroke(dashed);
				g.draw(new Line2D.Double(xP, 0, xP, getHeight()));
				g.draw(new Line2D.Double(xS, 0, xS, getHeight()));

				double x1 = getX((long) (arrivalP - Settings.pWaveInaccuracyThreshold));
				double x2 = getX((long) (arrivalP + Settings.pWaveInaccuracyThreshold));
				double x3 = getX((long) (arrivalS - Settings.pWaveInaccuracyThreshold));
				double x4 = getX((long) (arrivalS + Settings.pWaveInaccuracyThreshold));

				g.setColor(new Color(0, 0, 255, 80));
				g.fill(new Rectangle2D.Double(x1, 0, x2 - x1, h));

				g.setColor(new Color(255, 0, 0, 80));
				g.fill(new Rectangle2D.Double(x3, 0, x4 - x3, h));
			}
		}

		g.setColor(Color.black);
		g.setStroke(new BasicStroke(2f));
		g.draw(new Rectangle2D.Double(0, 0, w - 1, h - 1));
		g.draw(new Rectangle2D.Double(0, 0, w - 1, (h - 1) * 0.20));
		g.draw(new Rectangle2D.Double(0, h * 0.20, w - 1, (h - 1) * 0.20));
		g.draw(new Rectangle2D.Double(0, h * 0.40, w - 1, (h - 1) * 0.30));
		g.draw(new Rectangle2D.Double(0, h * 0.70, w - 1, (h - 1) * 0.30));

		g.dispose();

		this.image = img;
	}

	private long getTime() {
		return System.currentTimeMillis();
	}

	private double getX(long time) {
		return getWidth() * (1 - (getTime() - time) / (Settings.logsStoreTimeMinutes * 60 * 1000.0));
	}

	@Override
	public void paint(Graphics gr) {
		super.paint(gr);
		Graphics2D g = (Graphics2D) gr;
		g.drawImage(image, 0, 0, null);
	}

}
