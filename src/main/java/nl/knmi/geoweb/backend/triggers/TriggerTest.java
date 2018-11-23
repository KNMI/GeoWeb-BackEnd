package nl.knmi.geoweb.backend.triggers;

import nl.knmi.adaguc.tools.Tools;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.locationtech.jts.util.Debug;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

import javax.servlet.http.HttpServlet;
import java.io.*;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.*;

@RestController
@RequestMapping("/triggers")
public class TriggerTest extends HttpServlet {

    private static String unit = null;
    private static String variable = null;
    public static String activetriggerjsonpath = null;
    public static String triggerjsonpath = null;
    public static String triggerPath = "/nobackup/users/schouten/Triggers/";
    public static String activeTriggerPath = triggerPath + "ActiveTriggers/";
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

    @RequestMapping(path= "/triggercalculate", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static JSONArray calculateTrigger() throws IOException, InvalidRangeException, ParseException {

        // Everytime new triggers are calculated the old ones get deleted
        File folder = new File(triggerPath);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].getName().endsWith(".json")) {
                listOfFiles[i].delete();
            }
        }

        // Getting a list of filenames of all active triggers
        File activefolder = new File(activeTriggerPath);
        File[] listOfActiveFiles = activefolder.listFiles();
        JSONArray files = new JSONArray();
        for (int i = 0; i < listOfActiveFiles.length; i++) {
            if (listOfActiveFiles[i].isFile()) {
                files.add(listOfActiveFiles[i].getName());
            }
        }

        // Reading the latest dataset
        NetcdfFile hdf = NetcdfDataset.open(setDataset());
        station = hdf.readSection("stationname");

        JSONParser parser = new JSONParser();

        JSONArray triggerResults = new JSONArray();

        // For every active trigger json file...
        for(int i = 0; i < files.size(); i++) {
            // ...getting the json object
            JSONObject triggerFile = (JSONObject) parser.parse(new FileReader(activeTriggerPath + String.valueOf(files.get(i))));
            JSONObject phen = (JSONObject) triggerFile.get("phenomenon");

            Object par = phen.get("parameter");
            Object operator = phen.get("operator");
            Object limit = phen.get("limit");

            data = hdf.readSection(String.valueOf(par));
            lat = hdf.readSection("lat");
            lon = hdf.readSection("lon");
            code = hdf.readSection("station");

            locarray = new JSONArray();

            // ...calculating if trigger exceeds the value in the dataset
            if (operator.equals("higher")) {
                for (int x = 0; x < station.getSize(); x++) {
                    if (data.getDouble(x) >= (double) limit) {
                        printed = true;
                        createJSONObject(x);
                        jsoncreated = true;
                    }
                }
            } else if (operator.equals("lower")) {
                for (int x = 0; x < station.getSize(); x++) {
                    if (data.getDouble(x) <= (double) limit && data.getDouble(x) >= -100) {
                        printed = true;
                        createJSONObject(x);
                        jsoncreated = true;
                    }
                }
            }
            if (!printed) {
                printed = true;
            }

            if (jsoncreated) {

                // Path + name with a random UUID where the calculated trigger will be saved as a json file
                activetriggerjsonpath = triggerPath + "trigger_" + UUID.randomUUID() + ".json";

                // ...creating the calculated trigger json object
                if (locarray.size() != 0) {
                    JSONObject json = new JSONObject();
                    json.put("locations", locarray);
                    json.put("phenomenon", phen);
                    triggerResults.add(json);

                    // ...writing the calculated trigger json object to a file with a try catch
                    try (FileWriter file = new FileWriter(activetriggerjsonpath)) {
                        file.write(json.toJSONString());
                        file.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return triggerResults;
    }

    private static void createJSONObject(int x){
        JSONObject locations = new JSONObject();
        locations.put("lat", lat.getDouble(x));
        locations.put("lon", lon.getDouble(x));
        locations.put("name", station.getObject(x));
        locations.put("code", code.getObject(x));
        locations.put("value", data.getDouble(x));
        locarray.add(locations);
    }

    @RequestMapping(path= "/triggercreate", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static void addTrigger(@RequestBody String payload) throws IOException {

        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);
        System.out.println(payload);

        String par = triggerInfo.getString("parameter");
        String operator = triggerInfo.getString("operator");
        Double limit = triggerInfo.getDouble("limit");
        String source = triggerInfo.getString("source");

        NetcdfFile hdf = NetcdfDataset.open(setDataset());

        Group find = hdf.getRootGroup();

        if(source.equals("OBS")) {
            variable = hdf.findVariableByAttribute(find, "long_name", par).getName();
            unit = hdf.findAttValueIgnoreCase(hdf.findVariable(variable), "units", "units");
        }

        JSONObject json = new JSONObject();
        JSONObject phenomenon = new JSONObject();
        phenomenon.put("parameter", variable);
        phenomenon.put("long_name", par);
        phenomenon.put("operator", operator);
        phenomenon.put("limit", limit);
        phenomenon.put("unit", unit);
        phenomenon.put("source", source);

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

        triggerjsonpath = activeTriggerPath + "trigger_" + LocalDateTime.now().format(formatter) + ".json";  // Path + name where the trigger will be saved as a json file

        json.put("phenomenon", phenomenon);

        // Creating the json file with a try catch

        try (FileWriter file = new FileWriter(triggerjsonpath)) {
            file.write(json.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gets the parameters from a dataset when source is chosen in the Front-End
    @RequestMapping(path="/parametersget", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static String getParameters() throws IOException, NullPointerException {
        NetcdfFile hdf = NetcdfDataset.open(setDataset());

        JSONArray phenomena = new JSONArray();
        Array phen = null;

        List vars = hdf.getVariables();
        for(int i = 0; i < vars.size(); i++) {
            Variable var = (Variable) vars.get(i);
            if (var.getDimensions().size() == 2) {
                Attribute phenomenon = var.findAttributeIgnoreCase("long_name");
                try {
                    phen = phenomenon.getValues();
                } catch (NullPointerException e) {
                    Debug.print("Some variables don't have a long_name");
                }
                phenomena.add(String.valueOf(phen).substring(0, String.valueOf(phen).length() - 1));
            }
        }

        return String.valueOf(phenomena);
    }

    // Gets the unit of a parameter when chosen in the Front-End
    @RequestMapping(path="/unitget", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static String getUnit(@RequestBody String payload) throws IOException {

        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);

        String parameter = triggerInfo.getString("parameter");

        NetcdfFile hdf = NetcdfDataset.open(setDataset());

        Group find = hdf.getRootGroup();
        String variable = hdf.findVariableByAttribute(find, "long_name", parameter).getName();
        String value = hdf.findAttValueIgnoreCase(hdf.findVariable(variable), "units", "units");

        JSONObject unit = new JSONObject();

        unit.put("unit", value);

        return String.valueOf(unit);
    }

    // Gets all active triggers that are in the active trigger path
    @RequestMapping(path="/gettriggers", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    private static String getTriggers() throws IOException {
        String trigger;
        ArrayList triggerInfoList = new ArrayList();
        File folder = new File(activeTriggerPath);
        File[] listOfFiles = folder.listFiles();
        files = new JSONArray();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                files.add(listOfFiles[i].getName());
            }
        }
        for(int i = 0; i < files.size(); i++) {
            trigger = Tools.readFile(activeTriggerPath + files.get(i));
            triggerInfoList.add(trigger);
        }
        return String.valueOf(triggerInfoList);
    }

    // Sets the dataset to use trigger calculations on with the date in the name of the dataset
    public static String setDataset() throws IOException {
        String url;

        int year = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.YEAR);
        int month = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.MONTH) + 1;
        int day = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.DAY_OF_MONTH);
        int hour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY);
        int minutes = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.MINUTE)/10;
        String hours = String.valueOf(hour);
        String days = String.valueOf(day);
        String months = String.valueOf(month);

        if (String.valueOf(month).length() < 2) {
            months = String.format("%02d", month);
        }
        if (String.valueOf(day).length() < 2) {
            days = String.format("%02d", day);
        }
        if (String.valueOf(hour).length() < 2) {
            hours = String.format("%02d", hour);
        }
        if (minutes == 0) {
            hours = String.valueOf(Integer.parseInt(hours) -1);
            minutes = 5;
            if (hours.equals("00")) {
                hours = "23";
            }
        }

        url = "http://birdexp07.knmi.nl/geoweb/data/OBS/kmds_alle_stations_10001_" + year + months + days + hours + minutes + "0.nc";

        try{
            NetcdfDataset.open(url);
        } catch(FileNotFoundException e) {
            minutes = minutes - 1;
            url = "http://birdexp07.knmi.nl/geoweb/data/OBS/kmds_alle_stations_10001_" + year + months + days + hours + minutes + "0.nc";
            NetcdfDataset.open(url);
        }
        return url;
    }
}
