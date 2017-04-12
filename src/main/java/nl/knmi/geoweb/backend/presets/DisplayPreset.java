package nl.knmi.geoweb.backend.presets;

import lombok.Getter;

@Getter
public class DisplayPreset  {
		private String type;
		private int npanels;
    public DisplayPreset(){}
    
	public DisplayPreset(String type, int npanels) {
		this.type=type;
		this.npanels=npanels;
	}
}