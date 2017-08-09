package nl.knmi.geoweb.backend.product.taf;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.Getter;
import lombok.Setter;
import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFDefines.TAFCloudTypeName;
import nl.knmi.geoweb.backend.product.taf.Taf.TAFDefines.TAFWeather;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Taf {
	private String uuid = null;
	
	public static class TAFDefines {
		public enum WWModifier {LIGHT, NORMAL,HEAVY};
		@Getter
		@Setter
		public static class TAFWeather {
			String value;

			TAFWeather(String value) {
				this.value=value;
			}
			public TAFWeather() {}

			@JsonValue			
			public String getValue() {
				return value;
			}

			@Override
			public String toString() {
				return this.value;
			}
		}

		public static class TAFWeatherSet {
			//weather codes should be fetched from http://codes.wmo.int/306/_4678
			private static Map<String,TAFWeather> values=new HashMap<String,TAFWeather>();
			static {
				init();
			}
			private static void init() {
				values.put("MI", new TAFWeather("MI"));
				values.put("BC", new TAFWeather("BC"));
				values.put("PR", new TAFWeather("PR"));
				values.put("SHRA", new TAFWeather("SHRA"));
				values.put("+SHRA", new TAFWeather("+SHRA"));
				values.put("TSRA", new TAFWeather("TSRA"));
			}

			public static TAFWeather getValueOf(String s) {
				return values.get(s);
			}
		}

		public enum TAFCloudModifier {
			CB,TCU;
		}
		public enum TAFCloudTypeName {
			FEW,SCT,BKN,OVC;
		}
		public enum TAFChangeType {
			FM,BECMG,TEMPO,PROB30,PROB40,PROB30_TEMPO,PROB40_TEMPO;
		}
		public enum TAFWindUnits {
			M_S, KT,MPH;
		}
	}
	@Getter
	@Setter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TAFCloudType {
		Boolean isNSC;
		TAFDefines.TAFCloudTypeName type;
		TAFDefines.TAFCloudModifier mod;
		Integer h;

		public TAFCloudType() {this.isNSC=false;}

		public TAFCloudType(String cld) {
			if ("NSC".equalsIgnoreCase(cld)) {
				isNSC=true;
			} else {
				isNSC=null;
				String clouds="";
				for (TAFCloudTypeName name:TAFCloudTypeName.values()){
					if (clouds.length()>0) {
						clouds+="|"+name.toString();
					} else {
						clouds=name.toString();
					}
				}
				Pattern r=Pattern.compile("("+clouds+")(\\d{0,3})");
				Matcher m = r.matcher(cld);
				if (m.find()) {
					type=TAFDefines.TAFCloudTypeName.valueOf(m.group(1));
					h=Integer.parseInt(m.group(2))*100; //FT

					if (cld.contains("CB")) {
						mod=TAFDefines.TAFCloudModifier.valueOf("CB");
					} else if (cld.contains("TCU")) {
						mod=TAFDefines.TAFCloudModifier.valueOf("TCU");
					}
				}
			}
		}

		public String toTAC() {
			StringBuilder sb=new StringBuilder();
			if (isNSC!=null&&isNSC){
				sb.append("NSC");
			} else {
				sb.append(type.toString());
				sb.append(String.format("%03d", h/100));
				if (mod!=null) {
					sb.append(mod);
				}
			}
			return sb.toString();
		}
	}

	@Getter
	@Setter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TAFWind {
		@JsonProperty("isVariable")
		Boolean isVariable;
		Integer direction;
		Integer speed;
		Integer gusts;
		TAFDefines.TAFWindUnits units;

		public TAFWind(){}

		public TAFWind(boolean isVariable, Integer direction, Integer speed, Integer gusts, String units) {
			this.direction=direction;
			this.speed=speed;
			this.gusts=gusts;
			this.isVariable=isVariable;
			this.units=TAFDefines.TAFWindUnits.valueOf(units.toUpperCase());
		}
		public String toTAC() {
			StringBuilder sb=new StringBuilder();
			
			if (isVariable) {
				sb.append("VRB");
			} else {
				sb.append(String.format("%03d", direction));
			}
			sb.append(String.format("%02d", speed));
			if (gusts!=null) {
				sb.append(String.format("G%02d", gusts));
			}
			sb.append(units.toString());
			return sb.toString();
		}
	}

	@Getter
	@Setter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TAFVisibility {
		Integer visibilityRange;

		public TAFVisibility() {}

		public TAFVisibility(Integer visibilityRange) {
			this.visibilityRange=visibilityRange;
		}
	}

	@Getter
	@Setter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public class TAFTemperature {
		float maxTemperature;
		OffsetDateTime maxTime;
		float minTemperature;
		OffsetDateTime minTime;

		public TAFTemperature() {}
	}

	@Setter
	@Getter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	//	@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, 
	//		      include=JsonTypeInfo.As.PROPERTY, property="@type")
	//	@JsonSubTypes({
	//		@Type(value=Forecast.class),
	//		@Type(value=ChangeForecast.class)
	//	})
	public static class Forecast {
		@JsonDeserialize(as=ArrayList.class, contentAs=TAFDefines.TAFWeather.class)
		List<TAFDefines.TAFWeather> weather;
		@JsonDeserialize(as=ArrayList.class, contentAs=TAFCloudType.class)
		List<TAFCloudType>clouds;
		TAFVisibility visibility;
		TAFWind wind;
		TAFTemperature temperature;

		//		@JsonProperty("CaVOK")
		Boolean CaVOK;
		public Forecast() {
			this.CaVOK=false;
			this.weather=new ArrayList<TAFDefines.TAFWeather>();
			this.clouds=new ArrayList<TAFCloudType>();
			this.temperature=null;
		}

		public void addWeather(String ww) {
			//			if (this.weather==null) {
			//				this.weather=new ArrayList<TAFDefines.TAFWeather>();
			//			}
			CaVOK="NSW".equalsIgnoreCase(ww);
			this.weather.add(TAFDefines.TAFWeatherSet.getValueOf(ww));
		}

		public void addWind(TAFWind tafWind) {
			wind=tafWind;
		}


		public void addCloud(TAFCloudType cld) {
			this.clouds.add(cld);
		}

		public String toTAC() {
			StringBuilder sb=new StringBuilder();
			
			sb.append(getWind().toTAC());
			if (CaVOK) {
				sb.append(" CAVOK");
			} else {
				if (getVisibility().visibilityRange!=null) {
					sb.append(" "+String.format("%04d", getVisibility().visibilityRange));
				}
				if (getWeather()!=null) {
					for (TAFWeather w:getWeather() ) {
						sb.append(" "+w);
					}
				}

				if (getClouds()!=null){
					for (TAFCloudType tp: getClouds()) {
						sb.append(" ");
						sb.append(tp.toTAC());
					}
				}
			}	
			return sb.toString();
		}
	}

	@Getter
	@Setter
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class ChangeForecast extends Forecast{
		TAFDefines.TAFChangeType changeType;
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		OffsetDateTime changeStart;
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		OffsetDateTime changeEnd;
		Forecast forecast;

		public ChangeForecast() {super();}

		public ChangeForecast(String changeType, OffsetDateTime s, OffsetDateTime e) {
			super();
			this.changeType=TAFDefines.TAFChangeType.valueOf(changeType);
			this.changeStart=s;
			this.changeEnd=e;
		}
		@Override
		public String toTAC() {
			StringBuilder sb=new StringBuilder();
			sb.append(changeType.toString());
			sb.append(" "+toDDHH(changeStart));
			sb.append("/"+toDDHH(changeEnd));
			sb.append(" "+super.toTAC());
			return sb.toString();
		}
	}

	public enum TAFReportType {
		RETARDED, NORMAL, AMENDMENT, CANCEL, CORRECTION, MISSING;
	}
	
	public enum TAFReportPublishedConcept {
		CONCEPT, PUBLISHED
	}
	

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	OffsetDateTime issueTime;
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	OffsetDateTime validityStart;
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	OffsetDateTime validityEnd;
	Forecast forecast;
	@JsonDeserialize(as=ArrayList.class, contentAs=ChangeForecast.class)
	List<ChangeForecast> changeForecasts;
	String previousReportAerodrome;
	Period previousValidPeriod;
	TAFReportPublishedConcept status = TAFReportPublishedConcept.CONCEPT;
	TAFReportType type = TAFReportType.NORMAL;

	public Taf() {
		//		this.changeForecasts=new ArrayList<ChangeForecast>();
	}

	public Taf(String aerodrome, OffsetDateTime validityStart, OffsetDateTime validityEnd){
		this.previousReportAerodrome=aerodrome;
		this.validityStart=validityStart;
		this.validityEnd=validityEnd;
		this.issueTime=null;//OffsetDateTime.now(ZoneId.of("UTC")); //Update when publishing
		this.changeForecasts=new ArrayList<ChangeForecast>();
	}

	public void setForecast(Integer visibility, Integer windDir, Integer windSpd, Integer gust, String[] wws, boolean CaVOK, String[] clouds) {
		Forecast forecast=new Taf.Forecast();
		forecast.setWind(new TAFWind(false, windDir, windSpd, gust, "KT"));
		if (CaVOK) {
			forecast.CaVOK=true;
		} else {
			for (String ww:wws) {
				forecast.addWeather(ww);
			}
			for (String cld: clouds) {
				forecast.addCloud(new TAFCloudType(cld));
			}
			if (visibility!=null) {
				forecast.setVisibility(new TAFVisibility(visibility));
			}
		}
		this.forecast=forecast;
	}

	public void addChangeForecast(String changeType, OffsetDateTime s, OffsetDateTime e, Integer visibility, Integer windDir, Integer windSpd, Integer gust, String[] wws, boolean CaVOK, String[] clouds) {
		ChangeForecast changeForecast=new ChangeForecast(changeType, s, e);
		changeForecast.setWind(new TAFWind(false, windDir, windSpd, gust, "KT"));
		if (CaVOK) {
			changeForecast.CaVOK=true;
		} else {
			if (wws!=null) {
				for (String ww:wws) {
					changeForecast.addWeather(ww);
				}
			}
			if (clouds!=null) {
				for (String cld: clouds) {
					changeForecast.addCloud(new TAFCloudType(cld));
				}
			}
			if (visibility!=null) {
				changeForecast.setVisibility(new TAFVisibility(visibility));
			}
		}
		this.changeForecasts.add(changeForecast);
	}

	public static String toDDHHMM(OffsetDateTime t) {
		if(t==null)return null;
		DateTimeFormatter fmt= DateTimeFormatter.ofPattern("ddHHmm'Z'");
		return t.format(fmt);
	}

	public static String toDDHH(OffsetDateTime t) {
		DateTimeFormatter fmt= DateTimeFormatter.ofPattern("ddHH");
		return t.format(fmt);
	}

	public String toTAC() {
		StringBuilder sb=new StringBuilder();
		sb.append("TAF ");
		if (this.type == TAFReportType.AMENDMENT) {
			sb.append(" AMD ");
		}
		if (this.type == TAFReportType.CORRECTION) {
			sb.append(" COR ");
		}
		if (this.type == TAFReportType.RETARDED) {
			sb.append(" RTD ");
		}
		
		sb.append(previousReportAerodrome);
		sb.append(" "+toDDHHMM(issueTime));
		if (this.type == TAFReportType.MISSING) {
			sb.append(" NIL ");
		} else {
			sb.append(" "+toDDHH(validityStart)+"/"+toDDHH(validityEnd));
			if (this.type == TAFReportType.CANCEL) {
				sb.append("CNL");
			} else {
				sb.append(" "+forecast.toTAC());
				if (this.changeForecasts!=null) {
					for (ChangeForecast ch: this.changeForecasts) {
						sb.append("\n"+ch.toTAC());
					}
				}
			}
		}

		return sb.toString();
	}

	public String toJSON() throws JsonProcessingException {
		ObjectMapper om=getObjectMapperBean();	
		return om.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}
	
	public static Taf fromJSONString(String tafJson) throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper om=getObjectMapperBean();	
		Taf taf = om.readValue(tafJson, Taf.class);
		return taf;
	}


	public static Taf fromFile(File f) throws JsonParseException, JsonMappingException, IOException {
		return fromJSONString(Tools.readFile(f.getAbsolutePath()));
	}

    /** The standard date/time format used in JSON messages. */

    public static final String DATEFORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	// TODO use BEAN in proper way (Ask WvM)
	@Bean(name = "objectMapper")
    public static ObjectMapper getObjectMapperBean() {
        ObjectMapper om=new ObjectMapper();	
		om.registerModule(new JavaTimeModule());
		om.setTimeZone(TimeZone.getTimeZone("UTC"));
		om.setDateFormat(new SimpleDateFormat(DATEFORMAT_ISO8601));
		om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return om;

    }


	public static void main(String[]args) throws JsonProcessingException {
		Debug.println("start");
		OffsetDateTime s=OffsetDateTime.of(2017, 8, 4, 12, 0, 0, 0, ZoneOffset.UTC);
		OffsetDateTime e=OffsetDateTime.of(2017, 8, 5, 18, 0, 0, 0, ZoneOffset.UTC);
		Taf taf=new Taf("EHAM", s, e);
		taf.setForecast(9999, 200, 15, 25, new String[]{"SHRA","TSRA"}, true, new String[]{"FEW008", "SCT040", "OVC050"});

		OffsetDateTime c1_s=OffsetDateTime.of(2017, 8, 4, 16, 0, 0, 0, ZoneOffset.UTC);
		OffsetDateTime c1_e=OffsetDateTime.of(2017, 8, 4, 20, 0, 0, 0, ZoneOffset.UTC);
		taf.addChangeForecast("BECMG", c1_s, c1_e, 9999, 220, 17, 27, new String[]{"SHRA","TSRA"}, false, new String[]{"FEW009", "SCT041", "OVC051"} );
		taf.addChangeForecast("PROB30", c1_s, c1_e, 9999, 220, 17, 27, new String[]{"+SHRA"}, false, new String[]{"FEW009", "OVC470TCU"} );
		OffsetDateTime c3_s=OffsetDateTime.of(2017, 8, 5, 3, 0, 0, 0, ZoneOffset.UTC);
		OffsetDateTime c3_e=OffsetDateTime.of(2017, 8, 5, 5, 0, 0, 0, ZoneOffset.UTC);
		taf.addChangeForecast("BECMG", c3_s, c3_e, 9999, 200, 7, 17, new String[]{"SHRA","TSRA"}, false, new String[]{"FEW011", "SCT027"} );
		String taf_s=taf.toTAC();
		System.err.println("'"+taf_s+"'");
		System.err.println("JSON:"+taf.toJSON());
		String tafjson=taf.toJSON();

		ObjectMapper om=getObjectMapperBean();
		try {
			Taf newTaf=om.readValue(tafjson, Taf.class);
			String taf_s2=newTaf.toTAC();
			System.err.println("'"+taf_s2+"'");
			System.err.println("EQUAL: "+taf_s2.equals(taf_s));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	
}
