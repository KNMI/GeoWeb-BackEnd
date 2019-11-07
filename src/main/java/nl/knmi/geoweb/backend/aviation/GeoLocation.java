package nl.knmi.geoweb.backend.aviation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoLocation {
    private String EPSG;
    private Double lat;
    private Double lon;

    public GeoLocation(String epsg, double lat, double lon) {
        this.EPSG = epsg;
        this.lat = lat;
        this.lon = lon;
    }
}
