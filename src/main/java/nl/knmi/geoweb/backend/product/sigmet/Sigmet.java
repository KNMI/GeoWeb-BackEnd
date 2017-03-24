package nl.knmi.geoweb.backend.product.sigmet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sigmet {
	@Getter
	public enum Phenomenon {
		OBSC_TS("OBSC TS", "Obscured Thunderstorms"),OBSC_TSGR("OBSC TSGR", "Obscured Thunderstorms with hail"),
		EMBD_TS("EMBD TS", "Embedded Thunderstorms"),EMBD_TSGR("EMBD TSGR", "Embedded Thunderstorms with hail"),
		FRQ_TS("FRQ TS", "Frequent Thunderstorms"),FRQ_TSGR("FRQ TSGR", "Frequent Thunderstorms with hail"),
		SQL_TS("SQL TS", "Squall line"),SQL_TSGR("SQL TSGR", "Squall line with hail"),
		SEV_TURB("SEV TURB", "Severe Turbulence"),
		SEV_ICE("SEV ICE", "Severe Icing"), SEV_ICE_FRZA("SEV ICE (FRZA)", "Severe Icing with Freezing Rain"),
		SEV_MTW("SEV MTW", "Severe Mountain Wave"),
		HVY_DS("HVY DS", "Heavy Duststorm"),HVY_SS("HVY SS", "Heavy Sandstorm"),
		RDOACT_CLD("RDOACT CLD", "Radioactive Cloud")
		;
		private String description;
		private String shortDescription;
		
		public static Phenomenon getRandomPhenomenon() {
			int i=(int)(Math.random()*Phenomenon.values().length);
			System.err.println("rand "+i+ " "+Phenomenon.values().length);
			return Phenomenon.valueOf(Phenomenon.values()[i].toString());
		}
		
		private Phenomenon(String shrt, String description) {
			this.shortDescription=shrt;
			this.description=description;
		}
		public static Phenomenon getPhenomenon(String desc){
			for (Phenomenon phen: Phenomenon.values()) {
				if (desc.equals(phen.toString())){
					return phen;
				}
			}
			return null;
		}
	}
	
	@Getter
	public class ObsFc {
		private boolean obs=true ;
		Date obsFcTime;
		public ObsFc(boolean obs){
			this.obs=obs;
			this.obsFcTime=null;
		}
		public ObsFc(boolean obs, Date obsTime) {
			this.obs=obs;
			this.obsFcTime=obsTime;
		}
	}
	
	public enum SigmetLevelUnit {
		FT, FL, SFC, TOP, TOP_ABV;
	}

	@Getter
	public class SigmetLevelPart{
		public SigmetLevelPart(){
		}
		float value;
		SigmetLevelUnit unit;
		public SigmetLevelPart(SigmetLevelUnit unit, float val) {
			this.unit=unit;
			this.value=val;
		}
	}
	
	@Getter
	public class SigmetLevel {
		public SigmetLevel(){			
		}
		SigmetLevelPart lev1;
		SigmetLevelPart lev2;
		public SigmetLevel(SigmetLevelPart lev1) {
			this.lev1=lev1;
		}
		public SigmetLevel(SigmetLevelPart lev1, SigmetLevelPart lev2) {
			this.lev1=lev1;
			this.lev2=lev2;
		}
	}
	
	public enum SigmetDirection {
	  N,NNE,NE,ENE,E,ESE,SE,SSE,S,SSW,SW,WSW,W,WNW;
		public static SigmetDirection getSigmetDirection(String dir) {
			for (SigmetDirection v: SigmetDirection.values()) {
				if (dir.equals(v.toString())) return v;
			}
			return null;
		}
	}
	
	@Getter
	public class SigmetMovement {
	  private int speed;
	  private SigmetDirection dir;
	  private boolean stationary=true;
	  public SigmetMovement(boolean stationary) {
		  this.stationary=stationary;
	  }
	  public SigmetMovement(boolean stationary, String dir, int speed) {
		  this.stationary=stationary;
		  this.speed=speed;
		  this.dir=SigmetDirection.getSigmetDirection(dir);
	  }
	}
	
	@Getter
	public enum SigmetChange {
	    INTSF("Intensifying"), WKN("Weakening"), NC("No change");
	    private String description;
	    private SigmetChange(String desc) {
	    	this.description=desc;
	    }
	}
	
	@Getter
	public enum SigmetStatus {
		PRODUCTION, CANCELED, PUBLISHED; 
	}

	public static final long WSVALIDTIME = 4*3600*1000;
	public static final long WVVALIDTIME = 6*3600*1000;
	
	private String geo;
	private Phenomenon phenomenon;
	private ObsFc obs_or_forecast;
	private SigmetLevel level;
	private SigmetMovement movement;
	private SigmetChange change;
	
	private String forecast_position;
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ssZ")
	private Date issuedate;
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ssZ")
	private Date validdate;
	private String firname;
	private String icao_location_indicator;
	private String location_indicator_mwo;
	private String uuid;
	private SigmetStatus status;
	private int sequence;
	
	@Override
	public String toString() {
		ByteArrayOutputStream baos=new ByteArrayOutputStream();
		PrintStream ps=new PrintStream(baos);
		ps.println(String.format("Sigmet: %s %s %s [%s]", this.firname, icao_location_indicator, location_indicator_mwo, uuid));
		ps.println(String.format("seq: %d issued at %s valid from %s",sequence, this.issuedate, this.validdate));
		ps.println(String.format("change: %s geo: %s", this.change, this.geo));
		return baos.toString();
	}

	public Sigmet() {
	}
	
	public Sigmet(String firname, String location, String issuing_mwo, String uuid) {
		this.firname=firname;
		this.icao_location_indicator=location;
		this.location_indicator_mwo=issuing_mwo;
		this.uuid=uuid;
		this.sequence=-1;
		this.issuedate=new Date();
	}
	
	static int getRandomSequence=0;
	public static Sigmet getRandomSigmet() {
		Sigmet sm=new Sigmet("AMSTERDAM FIR", "EHAA", "EHDB", UUID.randomUUID().toString());
		sm.setPhenomenon(Phenomenon.getRandomPhenomenon());
		Date dt=new Date();
		dt.setTime(dt.getTime()+(int)(1000*3600*2*Math.random()));
		sm.setValiddate(new Date());
		sm.setChange(SigmetChange.NC);
		sm.setGeo("json string");
		sm.setSequence(getRandomSequence);
		getRandomSequence++;
		
		SigmetLevelPart p=sm.new SigmetLevelPart(SigmetLevelUnit.FL, 100);
		sm.setLevel(sm.new SigmetLevel(sm.new SigmetLevelPart(SigmetLevelUnit.FL, 100)));
		return sm;
	}
	
	public static Sigmet getSigmetFromFile(File f) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper om = new ObjectMapper();
		Sigmet sm=om.readValue(f, Sigmet.class);
		return sm;
	}
	
	public void serializeSigmet(String fn) {
		ObjectMapper om=new ObjectMapper();
		try {
			om.writeValue(new File(fn), this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String serializeSigmetToString() throws JsonProcessingException {
		ObjectMapper om=new ObjectMapper();
		return om.writeValueAsString(this);
	}
	
	public static void main(String args[]) {
		Sigmet sm=new Sigmet("AMSTERDAM FIR", "EHAA", "EHDB", "abcd");
		sm.setPhenomenon(Phenomenon.getPhenomenon("OBSC_TS"));
		sm.setValiddate(new Date(117,2,13,16,0));
		sm.setChange(SigmetChange.NC);
		sm.setGeo("json string");
		
		System.err.println(sm);
		final SigmetStore store =new SigmetStore("/tmp");
		for(int i=0;i<20;i++){
			sm = getRandomSigmet();
			store.storeSigmet(sm);
		}
		
	}
}
