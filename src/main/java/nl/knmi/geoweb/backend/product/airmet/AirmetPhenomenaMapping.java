package nl.knmi.geoweb.backend.product.airmet;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class AirmetPhenomenaMapping {
	public enum AirmetPhenomenonParamInfo {
		NEEDS_NONE, NEEDS_WIND, NEEDS_OBSCURATION, NEEDS_CLOUDLEVELS;
	}

	@Getter
	public class AirmetPhenomenon {
			String name;
			String code;
			AirmetPhenomenonParamInfo paraminfo;
			String layerpreset;
			public AirmetPhenomenon(String name, String code, String layerPreset) {
				this.name=name;
				this.code=code;
				this.layerpreset=layerPreset;
				this.paraminfo =AirmetPhenomenonParamInfo.NEEDS_NONE;
			}
			public AirmetPhenomenon(String name, String code, String layerPreset, AirmetPhenomenonParamInfo paraminfo) {
				this.name=name;
				this.code=code;
				this.layerpreset=layerPreset;
				this.paraminfo = paraminfo;
			}
		}
	private List<AirmetPhenomenon> phenomena;
	public AirmetPhenomenaMapping() {
		this.phenomena=new ArrayList<AirmetPhenomenon>();
		this.phenomena.add(new AirmetPhenomenon("Isolated thunderstorms",  "ISOL_TS", "Airmet_layer_TS"));
		this.phenomena.add(new AirmetPhenomenon("Isolated thunderstorms with hail",  "ISOL_TSGR", "Airmet_layer_TS"));
		this.phenomena.add(new AirmetPhenomenon("Occasional thunderstorms",  "OCNL_TS", "Airmet_layer_TS"));
		this.phenomena.add(new AirmetPhenomenon("Occasional thunderstorms with hail",  "OCNL_TSGR", "Airmet_layer_TS"));

		this.phenomena.add(new AirmetPhenomenon("Broken cloud",  "BKN_CLD", "Airmet_layer_CLD", AirmetPhenomenonParamInfo.NEEDS_CLOUDLEVELS));
		this.phenomena.add(new AirmetPhenomenon("Overcast cloud",  "OVC_CLD", "Airmet_layer_CLD", AirmetPhenomenonParamInfo.NEEDS_CLOUDLEVELS));
		this.phenomena.add(new AirmetPhenomenon("Frequent cumulonimbus cloud",  "FRQ_CB", "Airmet_layer_CLD"));
		this.phenomena.add(new AirmetPhenomenon("Frequent towering cumulus cloud",  "FRQ_TCU", "Airmet_layer_CLD"));
		this.phenomena.add(new AirmetPhenomenon("Isolated cumulonimbus cloud",  "ISOL_CB", "Airmet_layer_CLD"));
		this.phenomena.add(new AirmetPhenomenon("Isolated towering cumulus cloud",  "ISOL_TCU", "Airmet_layer_CLD"));
		this.phenomena.add(new AirmetPhenomenon("Occasional cumulonimbus cloud",  "OCNL_CB", "Airmet_layer_CLD"));
		this.phenomena.add(new AirmetPhenomenon("Occasional towering cumulus cloud",  "OCNL_TCU", "Airmet_layer_CLD"));

		this.phenomena.add(new AirmetPhenomenon("Moderate icing",  "MOD_ICE", "Airmet_layer_ICE"));
		this.phenomena.add(new AirmetPhenomenon("Moderate mountain wave",  "MOD_MTW", "Airmet_layer_WIND"));
		this.phenomena.add(new AirmetPhenomenon("Moderate turbulence",  "MOD_TURB", "Airmet_layer_TURB"));
		this.phenomena.add(new AirmetPhenomenon("Mountain obscuration",  "MT_OBSC", "Airmet_layer_CLD"));
		this.phenomena.add(new AirmetPhenomenon("Surface visibility",  "SFC_VIS", "Airmet_layer_VIS", AirmetPhenomenonParamInfo.NEEDS_OBSCURATION));
		this.phenomena.add(new AirmetPhenomenon("Surface wind",  "SFC_WIND", "Airmet_layer_WIND", AirmetPhenomenonParamInfo.NEEDS_WIND));
	}
}
