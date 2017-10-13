package nl.knmi.geoweb.backend.product.taf;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TAFtoTACMaps {
	private static final Map<String, String> qualifierMap;
	private static final Map<String, String> descriptorMap;
	private static final Map<String, String> phenomenaMap;
	
	static {
		qualifierMap = new HashMap<String, String>();
		descriptorMap = new HashMap<String, String>();
		phenomenaMap = new HashMap<String, String>();
		
		qualifierMap.put("moderate", "-");
		qualifierMap.put("heavy", "+");

		descriptorMap.put("shallow", "MI");
		descriptorMap.put("patches", "BC");
		descriptorMap.put("partial", "PR");
		descriptorMap.put("low drifting", "DR");
		descriptorMap.put("blowing", "BL");
		descriptorMap.put("showers", "SH");
		descriptorMap.put("thunderstorm", "TS");
		descriptorMap.put("freezing", "FZ");

		phenomenaMap.put("drizzle", "DZ");
		phenomenaMap.put("rain", "RA");
		phenomenaMap.put("snow", "SN");
		phenomenaMap.put("snow grains", "SG");
		phenomenaMap.put("ice pellets", "PL");
		phenomenaMap.put("hail", "GR");
		phenomenaMap.put("small hail", "GS");
		phenomenaMap.put("unknown precipitation", "UP");
		phenomenaMap.put("mist", "BR");
		phenomenaMap.put("fog", "FG");
		phenomenaMap.put("smoke", "FU");
		phenomenaMap.put("volcanic ash", "VA");
		phenomenaMap.put("widespread dust", "DU");
		phenomenaMap.put("sand", "SA");
		phenomenaMap.put("haze", "HZ");
		phenomenaMap.put("dust", "PO");
		phenomenaMap.put("squalls", "SQ");
		phenomenaMap.put("funnel clouds", "FC");
		phenomenaMap.put("sandstorm", "SS");
		phenomenaMap.put("duststorm", "DS");

	}

	public static String getQualifier(String qualifier) {
		return qualifierMap.get(qualifier.trim());
	}
	public static String getDescriptor(String descriptor) {
		return descriptorMap.get(descriptor.trim());
	}
	public static String getPhenomena(String phenomenon) {
		return phenomenaMap.get(phenomenon.trim());
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
	
}
