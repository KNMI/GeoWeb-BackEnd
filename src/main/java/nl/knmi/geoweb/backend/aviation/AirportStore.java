package nl.knmi.geoweb.backend.aviation;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Component
public class AirportStore {
    @Autowired
    @Qualifier("geoWebObjectMapper")
    private ObjectMapper om;

    private String airportFile;
    private String directory;
    private Map<String, AirportInfo> airportInfos;

    public AirportStore(@Value(value = "${geoweb.products.storeLocation}") String productstorelocation) throws IOException {
        String dir = productstorelocation + "/admin/config";
        File f = new File(dir);
        if (f.exists() == false) {
            Debug.errprintln("Creating airport store at [" + f.getAbsolutePath() + "]");
            Tools.mksubdirs(f.getAbsolutePath());
        }
        if (f.isDirectory() == false) {
            Debug.errprintln("Airport store location is not a directory");
            throw new NotDirectoryException("Airport store location is not a directory");
        }
        this.directory = dir;
        this.airportFile = "BREM_20160310.json";
    }

    public void initStore() throws IOException {
        this.airportInfos = new HashMap<String, AirportInfo>();
        File fn = new File(this.directory + "/" + this.airportFile);
        Debug.errprintln("fn:" + fn);
        if (fn.exists() && fn.isFile()) {

        } else {
            Debug.errprintln("No airportfile found, copying one from resources dir to " + this.directory);
            String s = Tools.readResource(this.airportFile);
            String airportText = String.format("%s/%s", this.directory, this.airportFile);
            Tools.writeFile(airportText, s);
        }

        try {
            AirportJsonRecord[] airports = om.readValue(fn, AirportJsonRecord[].class);
            for (AirportJsonRecord airport : airports) {
                try {
                    AirportInfo airportInfo = new AirportInfo(airport.getIcao(), airport.getName(), Float.parseFloat(airport.getLat()),
                            Float.parseFloat(airport.getLon()), (airport.getHeight().length() > 0) ? Float.parseFloat(airport.getHeight()) : 0);
                    airportInfos.put(airport.getIcao(), airportInfo);
                } catch (NumberFormatException e) {
                    Debug.println("Error parsing airport record " + airport.getIcao());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Debug.println("Found " + airportInfos.size() + " records of airportinfo");
    }

    public AirportInfo lookup(String ICAO) {
        if (airportInfos == null) {
            try {
                initStore();
            } catch (IOException e) {
                Debug.errprintln("ERROR: on AirportStore.lookup(" + ICAO + ")" + " " + e);
                return null;
            }
        }
        return airportInfos.get(ICAO);
    }
}
