package nl.knmi.geoweb.backend.presets;

import lombok.Getter;

@Getter
public class DisplayPreset  {
	@Getter
	public enum DisplayType {
		SINGLE("single",1),DUAL("dual",2),QUADCOL("quadcol", 4),QAUDUNEVEN("quaduneven",4);
		private String string;
		private int count;
		private DisplayType(String string, int count){
			this.string=string;
			this.count=count;
		}
		public int getCount(){
			return count;
		}
		
		@Override
		public String toString() {
			return string;
		}
	};
    private DisplayType type;
    
    public DisplayPreset(){}
    
	public DisplayPreset(DisplayType type) {
		this.type=type;
	}
}