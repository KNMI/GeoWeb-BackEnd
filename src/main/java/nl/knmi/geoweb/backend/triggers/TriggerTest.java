package nl.knmi.geoweb.backend.triggers;

import nl.knmi.adaguc.tools.Tools;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.locationtech.jts.util.Debug;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/triggers")
public class TriggerTest {

    private static String name = null;
    private static String unit = null;
    public static String triggerjsonpath = null;
    public static String triggerPath = "/nobackup/users/schouten/Triggers/";
    private static Array
            station = null,
            data = null,
            lat = null,
            lon = null,
            code = null;
    private static boolean printed = false;
    private static boolean jsoncreated = false;
    private static JSONArray locarray = null;
    private static JSONArray files = null;

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
        Group find = hdf.getRootGroup();
        String variable = hdf.findVariableByAttribute(find, "long_name", par).getName();
        data = hdf.readSection(String.valueOf(variable));
        lat = hdf.readSection("lat");
        lon = hdf.readSection("lon");
        code = hdf.readSection("station");
        name = hdf.findAttValueIgnoreCase(hdf.findVariable(variable), "long_name", "long_name");
        unit = hdf.findAttValueIgnoreCase(hdf.findVariable(variable), "units", "units");

        JSONObject json = new JSONObject();

        locarray = new JSONArray();

        JSONObject phenomenon = new JSONObject();
        phenomenon.put("parameter", variable);
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

            triggerjsonpath = triggerPath + "trigger_" + LocalDateTime.now().format(formatter) + ".json";  // Path + name where the trigger will be saved as a json file

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

    @RequestMapping(path="/triggerget", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static String getTrigger() throws IOException {
        String triggerFile = Tools.readFile(triggerjsonpath);
        return triggerFile;
    }

    @RequestMapping(path="/parametersget", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static String getParameters(@RequestBody String payload) throws IOException, NullPointerException {

        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);

        String path = triggerInfo.getString("serviceurl");

        NetcdfFile hdf = NetcdfDataset.open(path);

        JSONArray phenomena = new JSONArray();
        Array phen = null;

        List vars = hdf.getVariables();
        for(int i = 0; i < vars.size(); i++) {
            Variable var = (Variable) vars.get(i);
            Attribute phenomenon = var.findAttributeIgnoreCase("long_name");
            try {
                phen = phenomenon.getValues();
            }catch (NullPointerException e){
                Debug.print("Some variables don't have a long_name");
            }
            phenomena.add(String.valueOf(phen).substring(0, String.valueOf(phen).length() - 1));
        }
        removeThese(phenomena);

        return String.valueOf(phenomena);
    }

    @RequestMapping(path="/unitget", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static String getUnit(@RequestBody String payload) throws IOException {

        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);

        String path = triggerInfo.getString("serviceurl");
        String parameter = triggerInfo.getString("parameter");

        NetcdfFile hdf = NetcdfDataset.open(path);

        Group find = hdf.getRootGroup();
        String variable = hdf.findVariableByAttribute(find, "long_name", parameter).getName();
        String value = hdf.findAttValueIgnoreCase(hdf.findVariable(variable), "units", "units");

        JSONObject unit = new JSONObject();

        unit.put("unit", value);

        return String.valueOf(unit);
    }

    @RequestMapping(path="gettriggers", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    private static String getTriggers() throws IOException {
        String trigger;
        ArrayList triggerInfoList = new ArrayList();
        File folder = new File(triggerPath);
        File[] listOfFiles = folder.listFiles();
        files = new JSONArray();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                files.add(listOfFiles[i].getName());
            }
        }
        for(int i = 0; i < files.size(); i++) {
            trigger = Tools.readFile(triggerPath + files.get(i));
            triggerInfoList.add(trigger);
        }
        return String.valueOf(triggerInfoList);
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

    private static void removeThese( JSONArray phenomena) {
        phenomena.remove("Station id");
        phenomena.remove("time of measurement");
        phenomena.remove("Station name");
        phenomena.remove("station  latitude");
        phenomena.remove("station longitude");
        phenomena.remove("Station height");
        phenomena.remove("wawa Weather Code");
        phenomena.remove("Present Weather");
        phenomena.remove("wawa Weather Code for Previous 10 Min Interval");
        phenomena.remove("wawa Weather Code for Previous 10 Min Interval");
        phenomena.remove("ADAGUC Data Products Standard");
        phenomena.remove("ADAGUC Data Products Standard");
    }
}
