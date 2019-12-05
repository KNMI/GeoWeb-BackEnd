package nl.knmi.geoweb.backend.product.sigmet.geo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.fmi.avi.model.immutable.PointGeometryImpl;
import fi.fmi.avi.model.immutable.PolygonsGeometryImpl;
import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.geojson.Polygon;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

public class GeoUtils {

	private static GeometryFactory gf;
	private static ObjectMapper om;
	private static GeoJsonReader reader;
	private static GeoJsonWriter writer;

	private static GeometryFactory getGeometryFactory() {
		if (gf==null) {
			gf=new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING));
		}
		return gf;
	}

	private static GeoJsonWriter getWriter() {
		if (writer==null) {
			writer=new GeoJsonWriter();
		}
		return writer;
	}

	private static GeoJsonReader getReader() {
		if (reader==null) {
			reader=new GeoJsonReader(GeoUtils.getGeometryFactory());
		}
		return reader;
	}

	private static ObjectMapper getObjectMapper() {
		if (om==null) {
			om=new ObjectMapper();
		}
		return om;
	}

	public static Geometry jsonFeature2jtsGeometry(Feature F)  {
		try {
			ObjectMapper om=getObjectMapper();
			if (F.getGeometry()==null) {
				return null;
			}
			String json=om.writeValueAsString(F.getGeometry());
			return getReader().read(json);
		} catch(ParseException | JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static fi.fmi.avi.model.Geometry jsonFeature2FmiAviGeometry(Feature f) {
	   if (f.getGeometry() instanceof Polygon) {
           PolygonsGeometryImpl.Builder builder = new PolygonsGeometryImpl.Builder();
           List<LngLatAlt> lls = ((Polygon)f.getGeometry()).getExteriorRing();
           List<Double> extPoints=new ArrayList<>();
           for (LngLatAlt ll: lls) {
               extPoints.add(ll.getLatitude());
               extPoints.add(ll.getLongitude());
           }
           List<List<Double>> rings = new ArrayList<>();
           rings.add(extPoints);
           builder.setPolygons(rings);
           return builder.build();

       } else if (f.getGeometry() instanceof Point) {
	       PointGeometryImpl.Builder builder=new PointGeometryImpl.Builder();
	       LngLatAlt ll = ((Point)f.getGeometry()).getCoordinates();
	       List<Double> pts = Arrays.asList(ll.getLatitude(), ll.getLongitude());
	       builder.setPoint(pts);
	       return builder.build();
       }
	   return null;
    }

	public static Feature jtsGeometry2jsonFeature(Geometry g)  {
		Feature f=null;
		try {
			ObjectMapper om=getObjectMapper();
			String json=getWriter().write(g);
//			Debug.println(">>>>JSON:"+json);
			org.geojson.Geometry<Double> geo= om.readValue(json, org.geojson.Geometry.class);
			f=new Feature();
			f.setGeometry(geo);
		} catch(IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return f;
	}

	public static Feature merge(Feature f1, Feature f2) {
		Geometry g1=jsonFeature2jtsGeometry(f1);
		Geometry g2=jsonFeature2jtsGeometry(f2);

        Geometry gNew=g1.union(g2);
		Coordinate[] coords=gNew.getCoordinates();
		if (!Orientation.isCCW(coords)) {
			gNew=gNew.reverse();
		}
		Feature f=jtsGeometry2jsonFeature(gNew);
		return f;
	}

}
