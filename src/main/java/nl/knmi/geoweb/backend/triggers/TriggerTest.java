package nl.knmi.geoweb.backend.triggers;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

@RestController
@RequestMapping("/triggers")
public class TriggerTest {

    private static String name = null;
    private static String unit = null;
    public static String triggerjsonpath = null;
    private static Array
            station = null,
            data = null,
            lat = null,
            lon = null,
            code = null;
    private static boolean printed = false;
    private static boolean jsoncreated = false;
    private static JSONArray locarray = null;

    @RequestMapping(path= "/triggercreate", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static void addTrigger(@RequestBody String payload) throws IOException, InvalidRangeException {

        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);
        System.out.println(payload);

        String path = triggerInfo.getString("serviceurl");
        String par = triggerInfo.getString("parameter");
        String operator = triggerInfo.getString("operator");
        Double limit = triggerInfo.getDouble("limit");

        NetcdfFile hdf = NetcdfDataset.open(path);

        station = hdf.readSection("stationname");
        data = hdf.readSection(par);
        lat = hdf.readSection("lat");
        lon = hdf.readSection("lon");
        code = hdf.readSection("station");
        name = hdf.findAttValueIgnoreCase(hdf.findVariable(par), "long_name", "long_name");
        unit = hdf.findAttValueIgnoreCase(hdf.findVariable(par), "units", "units");

        JSONObject json = new JSONObject();

        locarray = new JSONArray();

        JSONObject phenomenon = new JSONObject();
        phenomenon.put("parameter", par);
        phenomenon.put("long_name", name);
        phenomenon.put("operator", operator);
        phenomenon.put("limit", limit);
        phenomenon.put("unit", unit);

        if(operator.equals("higher")) {
            for(int i = 0; i < station.getSize(); i++) {
                if (data.getDouble(i) >= limit) {
                    printed = true;
                    createJSONObject(i);
                    jsoncreated = true;
                }
            }
        }
        else if(operator.equals("lower")){
            for(int i = 0; i < station.getSize(); i++) {
                if (data.getDouble(i) <= limit && data.getDouble(i) >= -100) {
                    printed = true;
                    createJSONObject(i);
                    jsoncreated = true;
                }
            }
        }
        if(!printed){
            printed = true;
        }

        if(jsoncreated) {
            // Setting a format of the date and time with only numbers (to put in the name of the trigger file)

            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.YEAR, 4)
                    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                    .appendValue(ChronoField.DAY_OF_MONTH, 2)
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                    .appendValue(ChronoField.MILLI_OF_SECOND, 5)
                    .toFormatter();

            triggerjsonpath = "/nobackup/users/schouten/Triggers/trigger_" + LocalDateTime.now().format(formatter) + ".json";  // Path + name where the trigger will be saved as a json file
//            String triggerjsonfile = triggerjsonpath.substring(triggerjsonpath.lastIndexOf("/") + 1);   // The actual name of the created json file without the path (to print what the file is called)

            json.put("locations", locarray);
            json.put("phenomenon", phenomenon);

            // Creating the json file with a try catch

            try (FileWriter file = new FileWriter(triggerjsonpath)) {
                file.write(json.toJSONString());
                file.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RequestMapping(path="/triggerget")
    public static String getTrigger() {
        return triggerjsonpath;
    }

    private static void createJSONObject(int i){
        JSONObject locations = new JSONObject();
        locations.put("lat", lat.getDouble(i));
        locations.put("lon", lon.getDouble(i));
        locations.put("name", station.getObject(i));
        locations.put("code", code.getObject(i));
        locations.put("value", data.getDouble(i));
        locarray.add(locations);
    }
}
