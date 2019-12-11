package nl.knmi.geoweb.backend.product.sigmet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import org.geojson.Point;
import org.locationtech.jts.geom.Coordinate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Getter;
import lombok.Setter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.GeoWebProduct;
import nl.knmi.geoweb.backend.product.IExportable;
import nl.knmi.geoweb.backend.product.ProductConverter;
import nl.knmi.geoweb.backend.product.sigmetairmet.ObsFc;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetChange;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetLevel;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetMovement;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetType;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetUtils;
import nl.knmi.geoweb.backend.traceability.ProductTraceability;

@JsonInclude(Include.NON_NULL)
@Getter
@Setter
public class Sigmet implements GeoWebProduct, IExportable<Sigmet>{
	public static final Duration WSVALIDTIME = Duration.ofHours(4); //4*3600*1000;
	public static final Duration WVVALIDTIME = Duration.ofHours(6); //6*3600*1000;

	private GeoJsonObject geojson;
	private Phenomenon phenomenon;
	private ObsFc obs_or_forecast;
	//	@JsonFormat(shape = JsonFormat.Shape.STRING)
	//	private OffsetDateTime forecast_position_time;
	private SigmetAirmetLevel levelinfo;
	private SigmetMovementType movement_type;
	private SigmetAirmetMovement movement;
	private SigmetAirmetChange change;

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private OffsetDateTime issuedate;
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private OffsetDateTime validdate;
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private OffsetDateTime validdate_end;
	private String firname;
	private String location_indicator_icao;
	private String location_indicator_mwo;
	private String uuid;
	private SigmetAirmetStatus status;
	private SigmetAirmetType type;
	private int sequence;

	@JsonInclude(Include.NON_NULL)
	private Integer cancels;
	@JsonInclude(Include.NON_NULL)
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	private OffsetDateTime cancelsStart;

	@JsonInclude(Include.NON_NULL)
	@JsonDeserialize(as = VAExtraFields.class)
	private VAExtraFields va_extra_fields;

	@JsonInclude(Include.NON_NULL)
	@JsonDeserialize(as = TCExtraFields.class)
	private TCExtraFields tc_extra_fields;

	@JsonIgnore
	private Feature firFeature;

	@Getter
	public static class VAExtraFields {
		/* https://www.icao.int/APAC/Documents/edocs/WV-SIGMET.pdf */
		// TODO: Add TAC for CANCEL move_to
		// TODO: TAC should be truncated on 69 characters.
		public Volcano volcano;
		boolean no_va_expected;
		List <String> move_to;
		@Getter
		public static class Volcano {
			String name;
			List <Number> position;
			public String toTAC() {
				String volcanoName = (this.name != null && this.name.length() > 0) ? " MT " + this.name.toUpperCase() : "";
				String location = "";
				try {
					location = (position != null && position.size() == 2) ?
							" PSN " + Sigmet.convertLat(position.get(0).doubleValue())
							+" " + Sigmet.convertLon(position.get(1).doubleValue()) :
								"";
				}catch(Exception e){
					Debug.printStackTrace(e);
				}
				return  ((volcanoName.length() > 0 || location.length() > 0) ? "VA ERUPTION" : "") +
						volcanoName +
						location +
						((volcanoName.length() > 0 || location.length() > 0) ? " " : "");
			}
		}
		public String toTAC () {
			if (volcano != null ) {
				return volcano.toTAC();
			}
			return "";
		}
	}

	@Getter
	public static class TCExtraFields {
		TropicalCyclone tropical_cyclone;
		@Getter
		public static class TropicalCyclone {
			String name;
		}
	}

	@Getter
	public enum Phenomenon {
		OBSC_TS("OBSC TS", "Obscured Thunderstorms"),OBSC_TSGR("OBSC TSGR", "Obscured Thunderstorms with hail"),
		EMBD_TS("EMBD TS", "Embedded Thunderstorms"),EMBD_TSGR("EMBD TSGR", "Embedded Thunderstorms with hail"),
		FRQ_TS("FRQ TS", "Frequent Thunderstorms"),FRQ_TSGR("FRQ TSGR", "Frequent Thunderstorms with hail"),
		SQL_TS("SQL TS", "Squall line"),SQL_TSGR("SQL TSGR", "Squall line with hail"),
		SEV_TURB("SEV TURB", "Severe Turbulence"),
		SEV_ICE("SEV ICE", "Severe Icing"), SEV_ICE_FZRA("SEV ICE (FZRA)", "Severe Icing with Freezing Rain"),
		SEV_MTW("SEV MTW", "Severe Mountain Wave"),
		HVY_DS("HVY DS", "Heavy Duststorm"),HVY_SS("HVY SS", "Heavy Sandstorm"),
		RDOACT_CLD("RDOACT CLD", "Radioactive Cloud"),
		VA_CLD("VA CLD", "Volcanic Ash Cloud"), /* https://www.icao.int/APAC/Documents/edocs/sigmet_guide6.pdf, 3.2 Sigmet phenomena */
		TROPICAL_CYCLONE("TC", "Tropical Cyclone");

		private String description;
		private String shortDescription;

		public static Phenomenon getRandomPhenomenon() {
			int i=(int)(Math.random()*Phenomenon.values().length);
			Debug.errprintln("rand "+i+ " "+Phenomenon.values().length);
			return Phenomenon.valueOf(Phenomenon.values()[i].toString());
		}

		private Phenomenon(String shrt, String description) {
			this.shortDescription=shrt;
			this.description=description;
		}

		public static Phenomenon getPhenomenon(String desc) {
			for (Phenomenon phen: Phenomenon.values()) {
				if (desc.equals(phen.toString())){
					return phen;
				}
			}
			return null;
			//throw new Exception("You NOOB: Non existing pheonomenon!!!" + desc);
		}
	}

	public enum SigmetMovementType {
		STATIONARY, MOVEMENT, FORECAST_POSITION;
	}

	//	public enum SigmetLevelOperator {
	//		TOP, TOP_ABV;
	//	}


	@Override
	public String toString() {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		PrintStream ps=new PrintStream(baos);
		ps.println(String.format("Sigmet: %s %s %s [%s]", this.firname, location_indicator_icao, location_indicator_mwo, uuid));
		ps.println(String.format("seq: %d issued at %s valid from %s",sequence, this.issuedate, this.validdate));
		ps.println(String.format("change: %s geo: %s", this.change, this.geojson));
		return baos.toString();
	}

	public Sigmet() {
		this.sequence=-1;
	}

	public Sigmet(Sigmet otherSigmet) {
		this.firname=otherSigmet.getFirname();
		this.location_indicator_icao=otherSigmet.getLocation_indicator_icao();
		this.location_indicator_mwo=otherSigmet.getLocation_indicator_mwo();
		this.sequence=-1;
		this.phenomenon = otherSigmet.getPhenomenon();
		this.change = otherSigmet.getChange();
		this.geojson = otherSigmet.getGeojson();
		this.levelinfo = otherSigmet.getLevelinfo();
		this.movement = otherSigmet.getMovement();
		this.obs_or_forecast = otherSigmet.getObs_or_forecast();
		this.movement_type = otherSigmet.getMovement_type();
		//		this.forecast_position_time = otherSigmet.getForecast_position_time();
		this.validdate = otherSigmet.getValiddate();
		this.validdate_end = otherSigmet.getValiddate_end();
		this.issuedate = otherSigmet.getIssuedate();
		this.firFeature = otherSigmet.getFirFeature();
		this.type=otherSigmet.getType();
		this.va_extra_fields = otherSigmet.getVa_extra_fields();
		this.tc_extra_fields = otherSigmet.getTc_extra_fields();
	}

	public Sigmet(String firname, String location, String issuing_mwo, String uuid) {
		this.firname=firname;
		this.location_indicator_icao=location;
		this.location_indicator_mwo=issuing_mwo;
		this.uuid=uuid;
		this.sequence=-1;
		this.phenomenon = null;
		// If a SIGMET is posted, this has no effect
		this.status=SigmetAirmetStatus.concept;
		this.type=SigmetAirmetType.normal;
		this.change= SigmetAirmetChange.NC;
	}

	public static Sigmet getSigmetFromFile(ObjectMapper om, File f) throws JsonParseException, JsonMappingException, IOException {
		Sigmet sm=om.readValue(f, Sigmet.class);
		//		Debug.println("Sigmet from "+f.getName());
		//		Debug.println(sm.dumpSigmetGeometryInfo());
		return sm;
	}


	public void serializeSigmet(ObjectMapper om, String fn) {
		Debug.println("serializeSigmet to "+fn);
		if(this.geojson == null || this.phenomenon == null) {
			throw new IllegalArgumentException("GeoJSON and Phenomenon are required");
		}
		// .... value from constructor is lost here, set it explicitly. (Why?)
		if(this.status == null) {
			this.status = SigmetAirmetStatus.concept;
		}
		if(this.type == null) {
			this.type = SigmetAirmetType.normal;
		}
		if(this.change == null) {
			this.change = SigmetAirmetChange.NC;
		}
		try {
			om.writeValue(new File(fn), this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String serializeSigmetToString(ObjectMapper om) throws JsonProcessingException {
		return om.writeValueAsString(this);
	}

	public static String convertLat(double lat) {
		String latDM = "";
		if (lat < 0) {
			latDM = "S";
			lat = Math.abs(lat);
		} else {
			latDM = "N";
		}
		int degrees = (int)Math.floor(lat);
		latDM += String.format("%02d", degrees);
		double fracPart = lat - degrees;
		int minutes = (int)Math.round(fracPart * 60.0);
		latDM += String.format("%02d", minutes);
		return latDM;
	}

	public static String convertLon(double lon) {
		String lonDM = "";
		if (lon < 0) {
			lonDM = "W";
			lon = Math.abs(lon);
		} else {
			lonDM = "E";
		}
		int degreesLon = (int)Math.floor(lon);
		lonDM += String.format("%03d", degreesLon);
		double fracPartLon = lon - degreesLon;
		int minutesLon = (int)Math.round(fracPartLon * 60.0);
		lonDM += String.format("%02d", minutesLon);
		return lonDM;
	}

	public String pointToDMSString(LngLatAlt lnglat) {
		double lon = lnglat.getLongitude();
		double lat = lnglat.getLatitude();

		return Sigmet.convertLat(lat) + " " + Sigmet.convertLon(lon);
	}

	public String pointToDMSString(Coordinate coord) {
		double lon = coord.getOrdinate(Coordinate.X);
		double lat = coord.getOrdinate(Coordinate.Y);

		return Sigmet.convertLat(lat) + " " + Sigmet.convertLon(lon);
	}

	public String latlonToDMS(List<LngLatAlt> coords) {
		return coords.stream().map(lnglat -> this.pointToDMSString(lnglat)).collect(Collectors.joining(" - "));
	}

	public String latlonToDMS(Coordinate[] coords) {
		Arrays.stream(coords);
		return Arrays.stream(coords).map(coord -> this.pointToDMSString(coord)).collect(Collectors.joining(" - "));
	}

	public String toTAC() {
		if (this.firFeature!=null) {
			return this.toTAC(this.firFeature);
		}
		return "";
	}

	public String toTAC(Feature FIR) {
		String missGeom = "Missing geometry";
		GeoJsonObject effectiveStartGeometry = SigmetAirmetUtils.findStartGeometry(this.geojson);
		if ((effectiveStartGeometry==null)||(((Feature)effectiveStartGeometry).getProperty("selectionType")==null)) {
			return missGeom;
		}

		// If no  start geometry, return "Missing geometry"
		if(((Feature)effectiveStartGeometry).getProperty("selectionType").equals("point")){
			Point p = (Point)((Feature)effectiveStartGeometry).getGeometry();
			if (p.getCoordinates()==null) {
				return missGeom;
			}
		}else if(((Feature)effectiveStartGeometry).getProperty("selectionType").equals("box")){
			List<List<LngLatAlt>> coordinates = ((Polygon)((Feature)effectiveStartGeometry).getGeometry()).getCoordinates();
			if (coordinates==null || coordinates.size() == 0) {
				return missGeom;
			}
		}else if(!((Feature)effectiveStartGeometry).getProperty("selectionType").equals("fir")){			
			GeoJsonObject intersected= SigmetAirmetUtils.extractSingleStartGeometry(this.geojson);
			if (((Polygon)((Feature)intersected).getGeometry())==null) {
				return missGeom;
			}
			try {
				int sz=((Polygon)((Feature)intersected).getGeometry()).getCoordinates().get(0).size();
				if (sz<=7)  {
					effectiveStartGeometry = intersected; // Use intersection result
				}
			}
			catch(Exception e) {}
		}
		// If movement type = End position and no end geometry drawn, return "Missing end geometry"
		if (this.movement_type==SigmetMovementType.FORECAST_POSITION) {
			String missEndGeom = "Missing end geometry";
			GeoJsonObject endGeometry =  (Feature)this.findEndGeometry(((Feature)SigmetAirmetUtils.findStartGeometry(this.geojson)).getId());
			if ((endGeometry==null)||(((Feature)endGeometry).getProperty("selectionType")==null)) {
				return missEndGeom;
			}
			if(((Feature)endGeometry).getProperty("selectionType").equals("point")){
				Point p = (Point)((Feature)endGeometry).getGeometry();
				if (p.getCoordinates()==null) {
					return missEndGeom;
				}
			}else if(((Feature)endGeometry).getProperty("selectionType").equals("box")){
				List<List<LngLatAlt>> coordinates = ((Polygon)((Feature)endGeometry).getGeometry()).getCoordinates();
				if (coordinates==null || coordinates.size() == 0) {
					return missEndGeom;
				}
			}else if(!((Feature)endGeometry).getProperty("selectionType").equals("fir")){	
				GeoJsonObject intersectedEnd= (Feature)this.extractSingleEndGeometry();
				if (((Polygon)((Feature)intersectedEnd).getGeometry())==null) {
					return missEndGeom;
				}	
			}
		}

		StringBuilder sb = new StringBuilder();
		String validdateFormatted = String.format("%02d", this.validdate.getDayOfMonth()) + String.format("%02d", this.validdate.getHour()) + String.format("%02d", this.validdate.getMinute());
		String validdateEndFormatted = String.format("%02d", this.validdate_end.getDayOfMonth()) + String.format("%02d", this.validdate_end.getHour()) + String.format("%02d", this.validdate_end.getMinute());

		sb.append(this.location_indicator_icao).append(" SIGMET ").append(this.sequence).append(" VALID ").append(validdateFormatted).append('/').append(validdateEndFormatted).append(' ').append(this.location_indicator_mwo).append('-');
		sb.append('\n');

		sb.append(this.location_indicator_icao).append(' ').append(this.firname);


		if (this.cancels != null && this.cancelsStart != null) {
			String validdateCancelled = String.format("%02d", this.cancelsStart.getDayOfMonth()) + String.format("%02d", this.cancelsStart.getHour()) + String.format("%02d", this.cancelsStart.getMinute());

			sb.append(' ').append("CNL SIGMET ").append(this.cancels).append(" ").append(validdateCancelled).append('/').append(validdateEndFormatted);
			if (va_extra_fields != null && va_extra_fields.move_to != null && va_extra_fields.move_to.size() > 0) {
				sb.append(' ').append("VA MOV TO ").append(va_extra_fields.move_to.get(0)).append(" FIR");
			}
			return sb.toString();
		}
		sb.append('\n');
		/* Test or exercise */
		SigmetAirmetType type = this.type == null ? SigmetAirmetType.normal :this.type;
		switch(type) {
		case test:
			sb.append("TEST ");
			break;
		case exercise:
			sb.append("EXER ");
			break;
		default:
		}

		if (va_extra_fields != null) {
			sb.append(va_extra_fields.toTAC());
		}

        Debug.println("phen: " + this.phenomenon);
        if (this.phenomenon==null) {
            sb.append(""); //Empty string for missing phenomenon
        } else {
            sb.append(this.phenomenon.getShortDescription());
        }

		sb.append('\n');
		if (this.getObs_or_forecast()!=null) {
			sb.append(this.obs_or_forecast.toTAC());
			sb.append('\n');
		}
		sb.append(SigmetAirmetUtils.featureToTAC((Feature)effectiveStartGeometry, FIR));
		sb.append('\n');

		String levelInfoText=this.levelinfo.toTAC();
		if (!levelInfoText.isEmpty()) {
			sb.append(levelInfoText);
			sb.append('\n');
		}

		if (this.movement_type==null) {
			this.movement_type=SigmetMovementType.STATIONARY;
		}

		switch (this.movement_type) {
		case STATIONARY:
			sb.append("STNR ");
			break;
		case MOVEMENT:
			if (this.movement!=null) {
				sb.append(this.movement.toTAC());
				sb.append('\n');
			}
			break;
		case FORECAST_POSITION:
			// Present forecast_position geometry below
			break;
		}

		if (this.change!=null) {
			sb.append(this.change.toTAC());
			sb.append('\n');
		}

		if (this.movement_type==SigmetMovementType.FORECAST_POSITION) {
			OffsetDateTime fpaTime=this.validdate_end;
			sb.append("FCST AT ").append(String.format("%02d", fpaTime.getHour())).append(String.format("%02d", fpaTime.getMinute())).append("Z");
			sb.append('\n');
			sb.append(SigmetAirmetUtils.featureToTAC((Feature)this.findEndGeometry(((Feature)SigmetAirmetUtils.findStartGeometry(this.geojson)).getId()), FIR));

		} else {
			if (va_extra_fields !=null && va_extra_fields.no_va_expected) {
				OffsetDateTime fpaTime=this.validdate_end;
				sb.append("FCST AT ").append(String.format("%02d", fpaTime.getHour())).append(String.format("%02d", fpaTime.getMinute())).append("Z");
				sb.append(" NO VA EXP");
			}
		}


		return sb.toString();
	}

	private static String START="start";
	private static String END="end";
	private static String INTERSECTION="intersection";

	public List<GeoJsonObject> findIntersectableGeometries() {
		List<GeoJsonObject>objs=new ArrayList<GeoJsonObject>();
		FeatureCollection fc=(FeatureCollection)this.geojson;
		for (Feature f: fc.getFeatures()) {
			if ((f.getProperty("featureFunction")!=null)&&(f.getProperty("featureFunction").equals(START)||f.getProperty("featureFunction").equals(END))){
				objs.add(f);
			}
		}
		return objs;
	}

	public List<GeoJsonObject> findEndGeometries() {
		List<GeoJsonObject>objs=new ArrayList<GeoJsonObject>();
		FeatureCollection fc=(FeatureCollection)this.geojson;
		for (Feature f: fc.getFeatures()) {
			if ((f.getProperty("featureFunction")!=null)&&f.getProperty("featureFunction").equals(END)){
				objs.add(f);
			}
		}
		return objs;
	}

	public GeoJsonObject findEndGeometry(String relatesTo) {
		FeatureCollection fc=(FeatureCollection)this.geojson;
		for (Feature f: fc.getFeatures()) {
			if ((f.getProperty("featureFunction")!=null)&&f.getProperty("featureFunction").equals(END)){
				if ((f.getProperty("relatesTo")!=null)&&f.getProperty("relatesTo").equals(relatesTo)) {
					return f;
				}
			}
		}
		return null;
	}

	public GeoJsonObject extractSingleEndGeometry() {
		FeatureCollection fc=(FeatureCollection)this.geojson;
		for (Feature f: fc.getFeatures()) {
			if ((f.getProperty("featureFunction")!=null)&&f.getProperty("featureFunction").equals(END)){
				for (Feature f2: fc.getFeatures()) {
					if ((f2.getProperty("featureFunction")!=null)&&f2.getProperty("featureFunction").equals(INTERSECTION)&&f.getId().equals(f2.getProperty("relatesTo"))){
						return f2;
					}
				}
				return f;
			}
		}
		return null;
	}

	public void putIntersectionGeometry(String relatesTo, Feature intersection) {
		FeatureCollection fc=(FeatureCollection)this.geojson;

		//Remove old intersection for id if it exists
		List<Feature> toremove=new ArrayList<Feature>();
		for (Feature f: fc.getFeatures()) {
			if ((f.getProperty("relatesTo")!=null)&&f.getProperty("relatesTo").equals(relatesTo)){
				if ((f.getProperty("featureFunction")!=null)&&f.getProperty("featureFunction").equals(INTERSECTION)){
					toremove.add(f);
				}
			}
		}
		if (!toremove.isEmpty()) {
			fc.getFeatures().removeAll(toremove);
		}
		//Add intersection
		//		intersection.setId(UUID.randomUUID().toString());
		intersection.setId(relatesTo+"-i");
		intersection.getProperties().put("relatesTo", relatesTo);
		intersection.getProperties().put("featureFunction", INTERSECTION);
		fc.getFeatures().add(intersection);
	}

	public void putEndGeometry(String relatesTo, Feature newFeature) {
		FeatureCollection fc=(FeatureCollection)this.geojson;

		//Remove old endGeometry for id if it exists
		List<Feature> toremove=new ArrayList<Feature>();
		for (Feature f: fc.getFeatures()) {
			if ((f.getProperty("relatesTo")!=null)&&f.getProperty("relatesTo").equals(relatesTo)){
				if ((f.getProperty("featureFunction")!=null)&&f.getProperty("featureFunction").equals(END)){
					toremove.add(f);
				}
			}
		}

		if (!toremove.isEmpty()) {
			fc.getFeatures().removeAll(toremove);
		}
		//Add intersection
		//		newFeature.setId(UUID.randomUUID().toString());
		newFeature.getProperties().put("relatesTo", relatesTo);
		newFeature.getProperties().put("featureFunction", END);
		fc.getFeatures().add(newFeature);
	}

	public List<String>fetchGeometryIds() {
		List<String>ids=new ArrayList<String>();
		FeatureCollection fc=(FeatureCollection)this.geojson;
		for (Feature f: fc.getFeatures()) {
			if ((f.getId()!=null)) {
				ids.add(f.getId());
			}else {
				ids.add("null");
			}
		}
		return ids;
	}

	public void putStartGeometry(Feature newFeature) {
		FeatureCollection fc=(FeatureCollection)this.geojson;

		//Add intersection
		newFeature.getProperties().put("featureFunction", START);
		fc.getFeatures().add(newFeature);
	}

	public String dumpSigmetGeometryInfo() {
		StringWriter sw=new StringWriter();
		PrintWriter pw=new PrintWriter(sw);
		pw.println("SIGMET ");
		FeatureCollection fc=(FeatureCollection)this.geojson;
		for (Feature f: fc.getFeatures()) {
			pw.print((f.getId()==null)?"  ":f.getId());
			pw.print(" ");
			pw.print((f.getProperty("featureFunction")==null)?"  ":f.getProperty("featureFunction").toString());
			pw.print(" ");
			pw.print((f.getProperty("selectionType")==null)?"  ":f.getProperty("selectionType").toString());
			pw.print(" ");
			pw.print((f.getProperty("relatesTo")==null)?"  ":f.getProperty("relatesTo").toString());
			pw.println();
		}
		return sw.toString();
	}

	public String toJSON(ObjectMapper om) throws JsonProcessingException {
		return om.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}

	// Same as TAC, but maximum line with 69 chars where words (e.g. "OVC020CB") are not split
	// Also has a header and footer to the message
	//    private String __getPublishableTAC() {
	//        String line = "";
	//        String publishTAC = "";
	//        String[] TACwords = this.toTAC(this.getFirFeature()).split("\\s+");
	//        for(int i = 0; i < TACwords.length; ++i) {
	//            if (line.length() + TACwords[i].length() + 1 <= 69) {
	//                if (line.length() > 0) line += " ";
	//                line += TACwords[i];
	//            } else {
	//                publishTAC += line + '\n';
	//                line = TACwords[i];
	//            }
	//        }
	//        publishTAC += line;
	//        String time = this.getValiddate().format(DateTimeFormatter.ofPattern("ddHHmm"));;
	//
	//        String header = "WSNL31 " + this.getLocation_indicator_mwo() + " " + time +'\n';
	//        String footer = "=";
	//        return header + publishTAC + footer;
	//    }

	@Override
	public String export(File path, ProductConverter<Sigmet> converter, ObjectMapper om) {
		//		String s=converter.ToIWXXM_2_1(this);
		List<String> toDeleteIfError=new ArrayList<>(); //List of products to delete in case of error
		try {
			OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
			String time = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
			String validTime = this.getValiddate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"));

			String bulletinHeader = "";
			String iwxxmBulletinHeader = "";
			if (this.getPhenomenon() == Phenomenon.VA_CLD) {
				bulletinHeader = "WVNL31";
				iwxxmBulletinHeader = "LVNL31";
			} else if (this.getPhenomenon() == Phenomenon.TROPICAL_CYCLONE) {
				bulletinHeader = "WCNL31";  // TODO CHECK if WS is OK for TC
				iwxxmBulletinHeader = "LCNL31";
			} else {
				bulletinHeader = "WSNL31";
				iwxxmBulletinHeader = "LSNL31";
			}

			String TACName = bulletinHeader + this.getLocation_indicator_mwo() + "_" + validTime + "_" + time;
			String tacFileName=path.getPath() + "/" + TACName + ".tac";
			String TACHeaderTime = now.format(DateTimeFormatter.ofPattern("ddHHmm"));
			String TACHeaderLocation = this.getLocation_indicator_mwo();
			/* Create TAC header */
			String TACHeader = "ZCZC\n" + bulletinHeader + " " + TACHeaderLocation+" "+TACHeaderTime+"\n";
			/* Create TAC message */
			String TACCode = this.toTAC(this.getFirFeature());
			// Remove all empty lines
			TACCode  = TACCode.replaceAll("(?m)^[ \t]*\r?\n", "");
			// Replace last \n if available
			if (TACCode.length() > 1 && TACCode.endsWith("\n")) { TACCode = TACCode.substring(0, TACCode.length() - 1); }
			/* Create TAC footer */
			String TACFooter = "=\nNNNN\n";
			Tools.writeFile(tacFileName, TACHeader + TACCode +  TACFooter);
			toDeleteIfError.add(tacFileName);

			String name = "SIGMET_" + this.getLocation_indicator_mwo() + "_" + validTime + "_" + time;
			String jsonFileName=path.getPath() + "/" + name + ".json";
			Tools.writeFile(jsonFileName, this.toJSON(om));
			toDeleteIfError.add(jsonFileName);

			ProductTraceability.TraceProduct(status.toString(),"SIGMET", this.getUuid(), this.getLocation_indicator_mwo(), validTime, name);

			String iwxxmName="A_"+iwxxmBulletinHeader+this.getLocation_indicator_mwo()+this.getValiddate().format(DateTimeFormatter.ofPattern("ddHHmm"));
			if (status.equals(SigmetAirmetStatus.canceled)){
				iwxxmName+="CNL";
			}
			iwxxmName+="_C_"+this.getLocation_indicator_mwo()+"_"+time;
			String s=converter.ToIWXXM_2_1(this);
			if ("FAIL".equals(s)) {
			  Debug.println(" ToIWXXM_2_1 failed");
			  toDeleteIfError.stream().forEach(f ->  {Debug.println("REMOVING "+f); Tools.rm(f); });
			  return "ERROR: sigmet.ToIWXXM_2_1() failed";
			} else {
				Tools.writeFile(path.getPath() + "/" + iwxxmName + ".xml", s);
			}
		} catch (IOException | NullPointerException e) {
			toDeleteIfError.stream().forEach(f ->  {Debug.println("REMOVING "+f); Tools.rm(f); });
			return "ERROR: "+e.getMessage();
		}
		return "OK";
	}
}
