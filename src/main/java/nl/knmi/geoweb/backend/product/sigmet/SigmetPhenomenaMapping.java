package nl.knmi.geoweb.backend.product.sigmet;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.Phenomenon;

@Getter
public class SigmetPhenomenaMapping {
	@Getter
	public class SigmetPhenomenon {
		@Getter
		public class Phenomenon {
			String name;
			String code;
			String layerpreset;
			public Phenomenon(String name, String code, String layerPreset) {
				this.name=name;
				this.code=code;
				this.layerpreset=layerPreset;
			}
		}
		@Getter
		public class Variant {
			String name;
			String code;
			public Variant(String name, String code) {
				this.name=name;
				this.code=code;
			}	
		}
		@Getter
		public class Addition {
			String name;
			String code;
			public Addition(String name, String code) {
				this.name=name;
				this.code=code;
			}
		}
		private Phenomenon phenomenon;
		private List<Variant>variants;
		private List<Addition>additions;
		public SigmetPhenomenon(String name, String code, String layerPreset) {
			 this.phenomenon=new Phenomenon(name, code, layerPreset);
			 this.variants=new ArrayList<Variant>();
			 this.additions=new ArrayList<Addition>();
		}
		public void addVariant(String name, String code) {
			variants.add(new Variant(name, code));
		}
		public void addAddition(String name, String code) {
			additions.add(new Addition(name, code));
		}
	}
	private List<SigmetPhenomenon> phenomena;
	public SigmetPhenomenaMapping() {
		this.phenomena=new ArrayList<SigmetPhenomenon>();
		SigmetPhenomenon phen=new SigmetPhenomenon("Thunderstorm",  "TS", "sigmet_layer_TS");
		phen.addVariant("Obscured", "OBSC");
		phen.addVariant("Embedded",  "EMBD");
		phen.addVariant("Frequent", "FRQ");
		phen.addVariant("Squall line", "SQL");
		phen.addAddition("with hail",  "GR");
		phenomena.add(phen);
		phen=new SigmetPhenomenon("Severe Turbulence",  "SEV_TURB", "sigmet_layer_SEV_TURB");
		phenomena.add(phen);
		phen=new SigmetPhenomenon("Severe Icing",  "SEV_ICE", "sigmet_layer_SEV_ICE");
		phen.addAddition("due to freezing rain", "FRZA");
		phenomena.add(phen);
		phen=new SigmetPhenomenon("Duststorm", "DS", "sigmet_layer_DS");
		phenomena.add(phen);
		phen=new SigmetPhenomenon("Sandstorm", "SS", "sigmet_layer_SS");
		phenomena.add(phen);
		phen=new SigmetPhenomenon("Radioactive cloud", "RDOACT_CLD", "sigmet_layer_RDOACT_CLD");
		phenomena.add(phen);
	}

}
