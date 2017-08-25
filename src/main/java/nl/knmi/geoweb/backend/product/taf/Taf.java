package nl.knmi.geoweb.backend.product.taf;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.TimeZone;

import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.Getter;
import lombok.Setter;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.taf.serializers.CloudsSerializer;
import nl.knmi.geoweb.backend.product.taf.serializers.WeathersSerializer;

@Getter
@Setter
public class Taf {

	public static final String DATEFORMAT_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";

	public enum TAFReportType {
		retarded, normal, amendment, cancel, correction, missing;
	}

	public enum TAFReportPublishedConcept {
		concept, published
	}
	
	@Getter
	@Setter
	public static class Metadata {
		private String uuid = null;
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		OffsetDateTime issueTime;
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		OffsetDateTime validityStart;
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		OffsetDateTime validityEnd;
		String location;
		TAFReportPublishedConcept status = TAFReportPublishedConcept.concept;
		TAFReportType type = TAFReportType.normal;
	};
	public Metadata metadata;

	@Setter
	@Getter
	public static class Forecast {
		@Getter
		@Setter
		public static class TAFCloudType {
			Boolean isNSC=null;
			String amount;
			String mod;
			Integer height;

			public TAFCloudType() {this.isNSC=null;}

			public TAFCloudType(String cld) {
				if ("NSC".equalsIgnoreCase(cld)) {
					isNSC=true;
				} 
			}

			public String toTAC() {
				StringBuilder sb=new StringBuilder();
				if (isNSC!=null&&isNSC){
					sb.append("NSC");
				} else {
					sb.append(amount.toString());
					sb.append(String.format("%03d", height));
					if (mod!=null) {
						sb.append(mod);
					}
				}
				return sb.toString();
			}
		}
		@JsonSerialize(using = CloudsSerializer.class)
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		List<TAFCloudType>clouds;
		
		@Getter
		@Setter
		public static class TAFWeather {
			Boolean isNSW=null;
			String qualifier;
			String descriptor;
			List<String> phenomena;

			TAFWeather(String ww) {
				isNSW = true;
			}
			
			public TAFWeather() { isNSW = null; }

			public String toString () {
				StringBuilder sb = new StringBuilder();
				if(this.qualifier != null) {
					sb.append(TAFtoTACMaps.getQualifier(this.qualifier));
				}
				if(this.descriptor != null) {
					sb.append(TAFtoTACMaps.getDescriptor(this.descriptor));
				}
				if(this.phenomena != null) {
					for (String phenomenon : this.phenomena) {
						sb.append(TAFtoTACMaps.getPhenomena(phenomenon));
					}
				}
				return sb.toString();
			}
		}
		@JsonInclude(JsonInclude.Include.NON_EMPTY)
		@JsonSerialize(using = WeathersSerializer.class)
		List<TAFWeather> weather;
		
		@Setter
		@Getter
		public static class TAFVisibility {
			Integer value;
			String unit;
			public String toTAC() {
				if(unit == null) {
					return String.format(" %04d", value);
				}
				if(unit.equals("KM")) {
					return String.format(" %02d", value) + "KM";
				}
				throw new IllegalArgumentException("Unknown unit found for visibility");
			}
		}		
		TAFVisibility visibility;
		
		@Getter
		@Setter
		public static class TAFWind {
			Object direction;
			Integer speed;
			Integer gusts;
			String unit;

			public String toTAC() {
				StringBuilder sb=new StringBuilder();
				if (direction.toString().equals("VRB")) {
					sb.append("VRB");
				} else {
					sb.append(String.format("%03d", Integer.parseInt(direction.toString())));
				}
				sb.append(String.format("%02d", speed));
				if (gusts!=null) {
					sb.append(String.format("G%02d", gusts));
				}
				sb.append(unit.toString());
				return sb.toString();
			}
		}		
		TAFWind wind;
		
		@Getter
		@Setter
		public class TAFTemperature {
			float maxTemperature;
			OffsetDateTime maxTime;
			float minTemperature;
			OffsetDateTime minTime;
		}
		TAFTemperature temperature;

		Boolean CaVOK;

		/**
		 * Converts Forecast to TAC 
		 * @return String with TAC representation of Forecast
		 */
		public String toTAC() {
			StringBuilder sb=new StringBuilder();
			if(getWind() != null) {
				sb.append(getWind().toTAC());
			}
			if (CaVOK != null && CaVOK == true) {
				sb.append(" CAVOK");
			} else {
				if (visibility != null && visibility.value!=null) {
					sb.append(visibility.toTAC());
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
	Forecast forecast;

	@Getter
	@Setter
	public static class ChangeForecast extends Forecast{
		String changeType;
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		OffsetDateTime changeStart;
		@JsonFormat(shape = JsonFormat.Shape.STRING)
		OffsetDateTime changeEnd;
		Forecast forecast;
		@Override
		public String toTAC() {
			StringBuilder sb=new StringBuilder();
			sb.append(changeType.toString());
			sb.append(" "+TAFtoTACMaps.toDDHH(changeStart));
			sb.append("/"+TAFtoTACMaps.toDDHH(changeEnd));
			sb.append(" "+forecast.toTAC());
			return sb.toString();
		}
	}
	List<ChangeForecast> changegroups;

	public String toJSON() throws JsonProcessingException {
		ObjectMapper om=getTafObjectMapperBean();	
		return om.writerWithDefaultPrettyPrinter().writeValueAsString(this);
	}

	public static Taf fromJSONString(String tafJson) throws JsonParseException, JsonMappingException, IOException{
		ObjectMapper om=getTafObjectMapperBean();	
		Taf taf = om.readValue(tafJson, Taf.class);
		return taf;
	}

	public static Taf fromFile(File f) throws JsonParseException, JsonMappingException, IOException {
		return fromJSONString(Tools.readFile(f.getAbsolutePath()));
	}

	public String toTAC() {
		Taf taf = this;
		StringBuilder sb=new StringBuilder();
		sb.append("TAF ");
		switch(taf.metadata.type) {
		case amendment:
			sb.append(" AMD ");
			break;
		case correction:
			sb.append(" COR ");
			break;
		case retarded:
			sb.append(" RTD ");
			break;
		default: 
			// Append nothing here
			break;
		}

		sb.append(taf.metadata.location);
		sb.append(" "+TAFtoTACMaps.toDDHHMM(taf.metadata.issueTime));
		switch(taf.metadata.type) {
		case missing:
			// If missing, we're done here
			sb.append(" NIL");
			return sb.toString();
		default:
			// do nothing
			break;
		}
		sb.append(" "+TAFtoTACMaps.toDDHH(taf.metadata.validityStart)+"/"+TAFtoTACMaps.toDDHH(taf.metadata.validityEnd));
		switch(taf.metadata.type) {
		case cancel:
			// In case of a cancel there are no change groups so we're done here
			sb.append(" CNL");
			return sb.toString();
		default: 
			// do nothing
			break;
		}
		// Add the rest of the TAC
		sb.append(" "+taf.forecast.toTAC());
		if (taf.changegroups!=null) {
			for (ChangeForecast ch: taf.changegroups) {
				sb.append("\n"+ch.toTAC());
			}
		}

		return sb.toString();
	}

	// TODO use BEAN in proper way (Ask WvM)
	@Bean(name = "objectMapper")
	public static ObjectMapper getTafObjectMapperBean() {
		ObjectMapper om=new ObjectMapper();	
		om.registerModule(new JavaTimeModule());
		om.setTimeZone(TimeZone.getTimeZone("UTC"));
		om.setDateFormat(new SimpleDateFormat(DATEFORMAT_ISO8601));
		om.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
		return om;

	}
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
}
