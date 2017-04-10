package nl.knmi.geoweb.backend.product.sigmet;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class SigmetParameters {
	@Getter
    public class FirArea {
    	String location_indicator_icao;
    	String firname;
    	String areapreset;
    	public FirArea(String icao, String firName, String areaPreset) {
    		this.location_indicator_icao=icao;
    		this.firname=firName;
    		this.areapreset=areaPreset;
    	}
    }
	
	private float maxhoursofvalidity;
	private float hoursbeforevalidity;
	private List<FirArea> firareas;
	private String location_indicator_wmo;
	public SigmetParameters() {
		this.maxhoursofvalidity=4;
		this.hoursbeforevalidity=4;
		this.location_indicator_wmo="EHDB";
		this.firareas=new ArrayList<FirArea>();
		firareas.add(new FirArea("EHAA", "AMSTERDAM FIR", "NL_FIR"));
	}
	
}
