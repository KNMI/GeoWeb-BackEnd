package nl.knmi.geoweb.backend.product.taf;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TAFtoTACMaps {
	private static final Map<String, String> qualifierMap;
	private static final Map<String, String> descriptorMap;
	private static final Map<String, String> phenomenaMap;
	
	static {
		qualifierMap = new HashMap<String, String>();
		descriptorMap = new HashMap<String, String>();
		phenomenaMap = new HashMap<String, String>();
		
		qualifierMap.put("light", "-");
		qualifierMap.put("moderate", "");
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

	public static String toDDHHMM_Z(OffsetDateTime t) {
		if(t==null)return null;
		DateTimeFormatter fmt= DateTimeFormatter.ofPattern("ddHHmm'Z'");
		return t.format(fmt);
	}

	public static String toDDHHMM(OffsetDateTime t) {
		if(t==null)return null;
		DateTimeFormatter fmt= DateTimeFormatter.ofPattern("ddHHmm");
		return t.format(fmt);
	}

	public static String toDDHH(OffsetDateTime t) {
		DateTimeFormatter fmt= DateTimeFormatter.ofPattern("ddHH");
		return t.format(fmt);
	}

	public static String toDDHH24(OffsetDateTime t) {
		if ((t.getMinute()==0)&&(t.getHour()==0)) {
			OffsetDateTime tprev=t.minusDays(1);
			return String.format("%02d%02d", tprev.getDayOfMonth(),24);
		}
		DateTimeFormatter fmt= DateTimeFormatter.ofPattern("ddHH");
		return t.format(fmt);
	}

	private static String findPhenomena(String term) {
	    for (Map.Entry<String, String> e: phenomenaMap.entrySet()){
	        if (e.getValue().equals(term)){
	            return e.getKey();
            }
        }
		return null;
	}

	private static String findDescriptor(String term) {
	    for (Map.Entry<String, String> e: descriptorMap.entrySet()){
	        if (e.getValue().equals(term)){
	            return e.getKey();
            }
        }
		return null;
	}

    private static String findQualifier(String term) {
        for (Map.Entry<String, String> e: qualifierMap.entrySet()){
            if (e.getValue().equals(term)){
                return e.getKey();
            }
        }
        return null;
    }

	public static Taf.Forecast.TAFWeather fromTacString(String tac) {

	    Taf.Forecast.TAFWeather w=new Taf.Forecast.TAFWeather();
		String localTac=tac;
		if (tac.startsWith("-")) {
		    w.setQualifier(findQualifier("-"));
			localTac=localTac.substring(1);
		} else if (tac.startsWith("+")) {
		    w.setQualifier(findQualifier("+"));
			localTac=localTac.substring(1);
		}

        w.setPhenomena(new ArrayList<>());

        int cnt=0;
		int len=localTac.length();
		String firstTerm=localTac.substring(cnt,cnt+2);
		cnt=cnt+2;
		String descr=findDescriptor(firstTerm);
		if (descr!=null) {
		    w.setDescriptor(descr);
		    localTac=localTac.substring(2);
		    len=localTac.length();
        }

        for (int i=0; i<len; i=i+2) {
            String ph=localTac.substring(i, i+2);
            String phenomenon=findPhenomena(ph);
            if (phenomenon!=null){
                w.getPhenomena().add(phenomenon);
            } else {
                log.error("Unknown phenomenon "+ph);
            }
        }
        return w;
	}
}
