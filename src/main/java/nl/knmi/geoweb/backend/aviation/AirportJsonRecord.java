package nl.knmi.geoweb.backend.aviation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AirportJsonRecord {
  String icao;
  String icaoname;
  String name;
  String wmo;
  String fir;
  String iata;
  String lat;
  String lon;
  String height;
  String type;
}
