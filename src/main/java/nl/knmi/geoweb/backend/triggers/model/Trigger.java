package nl.knmi.geoweb.backend.triggers.model;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import nl.knmi.geoweb.backend.triggers.view.TriggerTransport;

public class Trigger {
	private TriggerPhenomenon phenomenon;
	private List<TriggerLocation> locations;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date triggerdate;
	private String uuid;
	private List<String> presets;

	public Trigger(
			@JsonProperty("phenomenon") TriggerPhenomenon phenomenon,
			@JsonProperty("locations") List<TriggerLocation> locations,
			@JsonProperty("triggerdate") Date triggerdate,
			@JsonProperty("uuid") String uuid
	) {
		this.phenomenon = phenomenon;
		this.locations = locations;
		this.triggerdate = triggerdate;
		this.uuid = uuid;
	}

	public Trigger(TriggerTransport transport, String uuid) {
		this(transport.getPhenomenon(), transport.getLocations(), transport.getTriggerdate(), uuid);
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

	public String getUuid() {
		return uuid;
	}

	/* TODO This seems to be an entirely separate concern. */
	public List<String> getPresets() {
		return presets;
	}

	public void setPresets(List<String> presets) {
		this.presets = presets;
	}
}
