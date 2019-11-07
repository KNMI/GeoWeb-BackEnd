package nl.knmi.geoweb.backend.aviation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AirportInfo {
    private String name;
    private String ICAOName;
    private GeoLocation geoLocation;
    private double fieldElevation;

    public AirportInfo(String ICAOName, String name, double lat, double lon, double elevation) {
        this(ICAOName, name, "EPSG:4326", lat, lon, elevation);
    }

    public AirportInfo(String ICAOName, String name, String epsg, double lat, double lon, double elevation) {
        this.ICAOName=ICAOName;
        this.name=name;
        this.geoLocation=new GeoLocation(epsg, lat, lon);
        this.fieldElevation=elevation;
    }

}
