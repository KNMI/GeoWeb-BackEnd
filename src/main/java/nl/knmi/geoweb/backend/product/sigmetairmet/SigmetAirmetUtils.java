package nl.knmi.geoweb.backend.product.sigmetairmet;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.Polygon;
import org.geojson.Point;
import org.geojson.LngLatAlt;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import lombok.extern.slf4j.Slf4j;

import org.locationtech.jts.io.ParseException;

@Slf4j
public class SigmetAirmetUtils {
  private static String START = "start";
  private static String INTERSECTION = "intersection";

  private static String convertLat(double lat) {
    String latDM = "";
    if (lat < 0) {
      latDM = "S";
      lat = Math.abs(lat);
    } else {
      latDM = "N";
    }
    int degrees = (int) Math.floor(lat);
    latDM += String.format("%02d", degrees);
    double fracPart = lat - degrees;
    int minutes = (int) Math.round(fracPart * 60.0);
    latDM += String.format("%02d", minutes);
    return latDM;
  }

  private static String convertLon(double lon) {
    String lonDM = "";
    if (lon < 0) {
      lonDM = "W";
      lon = Math.abs(lon);
    } else {
      lonDM = "E";
    }
    int degreesLon = (int) Math.floor(lon);
    lonDM += String.format("%03d", degreesLon);
    double fracPartLon = lon - degreesLon;
    int minutesLon = (int) Math.round(fracPartLon * 60.0);
    lonDM += String.format("%02d", minutesLon);
    return lonDM;
  }

  private static String pointToDMSString(LngLatAlt lnglat) {
    double lon = lnglat.getLongitude();
    double lat = lnglat.getLatitude();

    return convertLat(lat) + " " + convertLon(lon);
  }

  private static String pointToDMSString(Coordinate coord) {
    double lon = coord.getOrdinate(Coordinate.X);
    double lat = coord.getOrdinate(Coordinate.Y);

    return convertLat(lat) + " " + convertLon(lon);
  }

  private static String latlonToDMS(Coordinate[] coords) {
    Arrays.stream(coords);
    return Arrays.stream(coords).map(coord -> pointToDMSString(coord)).collect(Collectors.joining(" - "));
  }

  private static String latlonToDMS(List<LngLatAlt> coords) {
    return coords.stream().map(lnglat -> pointToDMSString(lnglat)).collect(Collectors.joining(" - "));
  }

  public static GeoJsonObject findStartGeometry(GeoJsonObject geojson) {
    FeatureCollection fc = (FeatureCollection) geojson;
    for (Feature f : fc.getFeatures()) {
      if ((f.getProperty("featureFunction") != null) && f.getProperty("featureFunction").equals(START)) {
        return f;
      }
    }
    return null;
  }

  public static GeoJsonObject extractSingleStartGeometry(GeoJsonObject geojson) {
    FeatureCollection fc = (FeatureCollection) geojson;
    for (Feature f : fc.getFeatures()) {
      if ((f.getProperty("featureFunction") != null) && f.getProperty("featureFunction").equals(START)) {
        for (Feature f2 : fc.getFeatures()) {
          if ((f2.getProperty("featureFunction") != null) && f2.getProperty("featureFunction").equals(INTERSECTION)
              && f.getId().equals(f2.getProperty("relatesTo"))) {
            return f2;
          }
        }
        return f;
      }
    }
    return null;
  }

  public static String featureToTAC(Feature f, Feature FIR) {
    List<LngLatAlt> coords;

    /* Empty text if feature or geometry is missing */
    if ((f == null) || 
      ((f.getGeometry()==null) && !f.getProperty("selectionType").toString().toLowerCase().equals("fir")))
      return "";

    switch (f.getProperty("selectionType").toString().toLowerCase()) {
    case "poly":
      // This assumes that one feature contains one set of coordinates
      coords = ((Polygon) (f.getGeometry())).getCoordinates().get(0);
      return "WI " + latlonToDMS(coords);
    case "fir":
      return "ENTIRE FIR";
    case "point":
      Point p = (Point) f.getGeometry();
      return pointToDMSString(p.getCoordinates());
    case "box":
      // A box is drawn which can mean multiple things whether how many intersections
      // there are.
      // If one line segment intersects, the phenomenon happens in the area opposite
      // of the line intersection
      // e.g. if the south border of the box intersects, the phenomenon happens north
      // of this line.
      // If there are multiple intersections -- we assume two currently -- the
      // phenomenon happens in the quadrant
      // opposite of the intersection lines.
      // E.g. the south and west border of the box intersect, the phenomenon happens
      // north of the south intersection line and east of the west intersection line
      GeometryFactory gf = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
      GeoJsonReader reader = new GeoJsonReader(gf);

      if (FIR == null || FIR.getGeometry() == null) {
        log.warn("FIR is null!!");
        return "";
      }
      try {
        ObjectMapper om = new ObjectMapper();
        String FIRs = om.writeValueAsString(FIR.getGeometry()); // FIR as String

        try {
          org.locationtech.jts.geom.Geometry drawnGeometry = reader.read(om.writeValueAsString(f.getGeometry()));

          org.locationtech.jts.geom.Geometry geom_fir = reader.read(FIRs);

          // Sort box's coordinates
          Envelope env = drawnGeometry.getEnvelopeInternal();
          double minX = env.getMinX();
          double maxX = env.getMaxX();
          double minY = env.getMinY();
          double maxY = env.getMaxY();

          if ((minX == maxX) || (minY == maxY))
            return " POINT "; // Box is one point!!

          // org.locationtech.jts.geom.Geometry firBorder = geom_fir.getBoundary();

          // Find intersections with box's sides
          CoordinateArraySequenceFactory caf = CoordinateArraySequenceFactory.instance();
          boolean[] boxSidesIntersecting = new boolean[4];
          int boxSidesIntersectingCount = 0;

          // Sort the rectangle points counterclockwise, starting at lower left
          Coordinate[] drawnCoords = new Coordinate[5];
          for (int i = 0; i < 4; i++) {
            if (drawnGeometry.getCoordinates()[i].x == minX) {
              if (drawnGeometry.getCoordinates()[i].y == minY) {
                drawnCoords[0] = drawnGeometry.getCoordinates()[i];
              } else {
                drawnCoords[3] = drawnGeometry.getCoordinates()[i];
              }
            } else {
              if (drawnGeometry.getCoordinates()[i].y == minY) {
                drawnCoords[1] = drawnGeometry.getCoordinates()[i];
              } else {
                drawnCoords[2] = drawnGeometry.getCoordinates()[i];
              }
            }
          }
          drawnCoords[4] = drawnCoords[0]; // Copy first point to last
          log.debug("drawnCoords: " + drawnCoords[0] + " " + drawnCoords[1] + " " + drawnCoords[2] + " "
              + drawnCoords[3] + " " + drawnCoords[4]);

          for (int i = 0; i < 4; i++) {
            LineString side = new LineString(caf.create(Arrays.copyOfRange(drawnCoords, i, i + 2)), gf);
            if (geom_fir == null)
              return " ERR (geom_fir) ";
            if (side.intersects(geom_fir)) {
              boxSidesIntersecting[i] = true;
              boxSidesIntersectingCount++;
            } else {
              boxSidesIntersecting[i] = false;
            }
          }

          log.debug("Intersecting box on " + boxSidesIntersectingCount + " sides");
          if (boxSidesIntersectingCount == 1) {
            if (boxSidesIntersecting[0]) {
              // N of
              return String.format("N OF %s", convertLat(minY));
            } else if (boxSidesIntersecting[1]) {
              // W of
              return String.format("W OF %s", convertLon(maxX));
            } else if (boxSidesIntersecting[2]) {
              // S of
              return String.format("S OF %s", convertLat(maxY));
            } else if (boxSidesIntersecting[3]) {
              // E of
              return String.format("E OF %s", convertLon(minX));
            }
          } else if (boxSidesIntersectingCount == 2) {
            if (boxSidesIntersecting[0] && boxSidesIntersecting[1]) {
              // N of and W of
              return String.format("N OF %s AND W OF %s", convertLat(minY), convertLon(maxX));
            } else if (boxSidesIntersecting[1] && boxSidesIntersecting[2]) {
              // S of and W of
              return String.format("S OF %s AND W OF %s", convertLat(maxY), convertLon(maxX));
            } else if (boxSidesIntersecting[2] && boxSidesIntersecting[3]) {
              // S of and E of
              return String.format("S OF %s AND E OF %s", convertLat(maxY), convertLon(minX));
            } else if (boxSidesIntersecting[3] && boxSidesIntersecting[0]) {
              // N of and E of
              return String.format("N OF %s AND E OF %s", convertLat(minY), convertLon(minX));
            } else if (boxSidesIntersecting[0] && boxSidesIntersecting[2]) {
              // N of and S of
              return String.format("N OF %s AND S OF %s", convertLat(minY), convertLat(maxY));
            } else if (boxSidesIntersecting[1] && boxSidesIntersecting[3]) {
              // E of and W of
              return String.format("E OF %s AND W OF %s", convertLon(minX), convertLon(maxX));
            }
          }

          // Intersect the box with the FIR
          org.locationtech.jts.geom.Geometry intersection = drawnGeometry.intersection(geom_fir);

          if (intersection.equalsTopo(geom_fir)) {
            return "ENTIRE FIR";
          }

          Coordinate[] drawn = drawnGeometry.getCoordinates();
          Coordinate[] intersected = intersection.getCoordinates();
          coords = ((Polygon) (f.getGeometry())).getCoordinates().get(0);
          if (intersected.length > 7) {
            log.warn("More than 7 in intersection!!");
            return "WI " + latlonToDMS(drawn);
          }
          return "WI " + latlonToDMS(intersected);
        } catch (ParseException pe) {
          // log.error(pe.getMessage());
        }
      } catch (JsonProcessingException e) {
        log.error(e.getMessage());
      }
      return " ERR ";
    default:
      return "";
    }
  }
}
