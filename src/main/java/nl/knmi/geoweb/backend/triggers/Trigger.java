package nl.knmi.geoweb.backend.triggers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Trigger {
	@Getter
	public static class TriggerPhenomenon {
		private String parameter;
		private String operator;
		private double threshold;
		private String units;
		private String source;
		public TriggerPhenomenon(){}
	}
	@Getter
	public static class TriggerLocation {
		private double lat;
		private double lon;
		private String name;
		private String code;
		private double value;
		public TriggerLocation(){}
		public TriggerLocation(double lat, double lon, String name, String code, double value){
			this.code=code;
			this.lat=lat;
			this.lon=lon;
			this.name=name;
			this.value=value;
		}
	}
	@Getter
	@Setter
	public static class TriggerTransport {
		private List<TriggerLocation> locations;
		private TriggerPhenomenon phenomenon;
//		@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'")
//		private Date triggerdate;
		public TriggerTransport(){}
	}
	List<TriggerLocation> locations;
	private String uuid;
	private List<String> presets;
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date triggerdate;
	private TriggerPhenomenon phenomenon;
	
	public Trigger() {
		this.locations=new ArrayList<TriggerLocation>();
	}
	
	public Trigger(TriggerPhenomenon phenomenon, List<TriggerLocation>triggerLocations, Date triggerdate, String uuid){
		this.uuid=UUID.randomUUID().toString();	
		this.locations=triggerLocations;
		this.phenomenon=phenomenon;
		this.triggerdate=triggerdate;
		this.uuid=uuid;
		 
	}
	public Trigger(TriggerTransport transport) {
		this(transport.getPhenomenon(), transport.getLocations(),new Date(), UUID.randomUUID().toString());
		
	}
}
