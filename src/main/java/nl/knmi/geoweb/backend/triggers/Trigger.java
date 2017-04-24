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
	private String group;
	private String phenomenon;
	private String triggername;
	private String uuid;
	private List<String> tasks;
	private List<String> presets;
	@JsonFormat(shape=JsonFormat.Shape.STRING, pattern="yyyy-MM-dd'T'HH:mm:ss'Z'")
	private Date issuedate;
	private String parameters;

	public Trigger() {
		this.uuid=UUID.randomUUID().toString();
		this.setPhenomenon("t2m");
		this.setGroup("heat");
		this.setTriggername("obs above 10");
		this.tasks=new ArrayList<String>(); 
		this.presets=new ArrayList<String>();
		this.issuedate=new Date();
		this.parameters=null;
		this.parameters="{\"var\": \"t2m\"}";
	}

}
