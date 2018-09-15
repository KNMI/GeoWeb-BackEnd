package nl.knmi.geoweb.backend.triggers.view;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.knmi.geoweb.backend.triggers.model.TriggerLocation;
import nl.knmi.geoweb.backend.triggers.model.TriggerPhenomenon;

public class TriggerTransport {
	private TriggerPhenomenon phenomenon;
	private List<TriggerLocation> locations;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date triggerdate;

	public TriggerTransport(
			@JsonProperty("phenomenon") TriggerPhenomenon phenomenon,
			@JsonProperty("locations") List<TriggerLocation> locations,
			@JsonProperty("triggerdate") Date triggerdate
	) {
		this.phenomenon = phenomenon;
		this.locations = locations;
		this.triggerdate = triggerdate;
	}

	public TriggerPhenomenon getPhenomenon() {
		return phenomenon;
	}

	public List<TriggerLocation> getLocations() {
		return locations;
	}

	public Date getTriggerdate() {
		return triggerdate;
	}
}
