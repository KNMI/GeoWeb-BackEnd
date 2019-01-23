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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/triggers")
public class TriggerService extends HttpServlet {

    private static String unit = null;
    private static String variable = null;
    public static String activeTriggerJsonPath = null;
    public static String triggerJsonPath = null;
    public static String triggerPath = "/tmp/triggering/";        // Home directory of the triggers
    public static String activeTriggerPath = triggerPath + "ActiveTriggers/";
    public static String inactiveTriggerPath = triggerPath + "InactiveTriggers/";
    private static Array
            station = null,
            data = null,
            lat = null,
            lon = null,
            code = null;
    private static boolean printed = false;
    private static boolean jsonCreated = false;
    private static JSONArray locArray = null;
    private static JSONArray files = null;

    private static Dataset dataset;

    // Calculating the actual trigger with the values of active triggers over the values in the latest dataset and writes it to a json file in the trigger path
    @RequestMapping(path="/calculatetrigger", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static JSONArray calculateTrigger() throws IOException, InvalidRangeException, ParseException {

        // Everytime new triggers are calculated the old ones are deleted
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

        JSONArray triggerResults = new JSONArray();

        // If there are active triggers
        if (files.size() > 0) {
            // Reading the latest dataset
            NetcdfFile hdf = NetcdfDataset.open(dataset.setDataset());

            station = hdf.readSection("stationname");

            JSONParser parser = new JSONParser();


            // For every active trigger json file...
            for (int i = 0; i < files.size(); i++) {
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

                locArray = new JSONArray();

                // ...calculating if trigger exceeds the value in the dataset
                if (operator.equals("higher")) {
                    for (int x = 0; x < station.getSize(); x++) {
                        if (data.getDouble(x) >= (double) limit) {
                            printed = true;
                            createLocationJSONObject(x);
                            jsonCreated = true;
                        }
                    }
                } else if (operator.equals("lower")) {
                    for (int x = 0; x < station.getSize(); x++) {
                        if (data.getDouble(x) <= (double) limit && data.getDouble(x) >= -100) {
                            printed = true;
                            createLocationJSONObject(x);
                            jsonCreated = true;
                        }
                    }
                }
                if (!printed) {
                    printed = true;
                }

                if (jsonCreated) {

                    // Path + name with a random UUID where the calculated trigger will be saved as a json file
                    activeTriggerJsonPath = triggerPath + "trigger_" + UUID.randomUUID() + ".json";

                    // ...creating the calculated trigger json object
                    if (locArray.size() != 0) {
                        JSONObject json = new JSONObject();
                        json.put("locations", locArray);
                        json.put("phenomenon", phen);
                        triggerResults.add(json);

                        // ...writing the calculated trigger json object to a file
                        writeJsonFile(activeTriggerJsonPath, json);
                    }
                }
            }
            hdf.close();
        }
        return triggerResults;
    }

    // Creating a json object for the locations of a calculated trigger
    private static void createLocationJSONObject(int x){
        JSONObject locations = new JSONObject();
        locations.put("lat", lat.getDouble(x));
        locations.put("lon", lon.getDouble(x));
        locations.put("name", station.getObject(x));
        locations.put("code", code.getObject(x));
        locations.put("value", data.getDouble(x));
        locArray.add(locations);
    }

    // Creates the trigger from values set in the Front-End and adds it to the active triggers path
    @RequestMapping(path= "/triggercreate", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static void addTrigger(@RequestBody String payload) throws IOException {

        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);
        System.out.println(payload);

        String par = triggerInfo.getString("parameter");
        String operator = triggerInfo.getString("operator");
        Double limit = triggerInfo.getDouble("limit");
        String source = triggerInfo.getString("source");

        NetcdfFile hdf = NetcdfDataset.open(dataset.setDataset());

        Group find = hdf.getRootGroup();

        if(source.equals("OBS")) {
            variable = hdf.findVariableByAttribute(find, "long_name", par).getName();
            unit = hdf.findAttValueIgnoreCase(hdf.findVariable(variable), "units", "units");
        }

        JSONObject json = new JSONObject();

        // Set random UUID to put in trigger file name and to identify trigger
        UUID randomUUID = UUID.randomUUID();

        JSONObject phenomenon = new JSONObject();
        phenomenon.put("parameter", variable);
        phenomenon.put("long_name", par);
        phenomenon.put("operator", operator);
        phenomenon.put("limit", limit);
        phenomenon.put("unit", unit);
        phenomenon.put("source", source);
        phenomenon.put("UUID", randomUUID.toString());

        // Path + name where the trigger will be saved as a json file
        triggerJsonPath = activeTriggerPath + "trigger_" + randomUUID + ".json";

        json.put("phenomenon", phenomenon);

        // Writing the JSON file
        writeJsonFile(triggerJsonPath, json);

        hdf.close();
    }

    // JSON file writer with path and JSON object
    private static void writeJsonFile(String path, JSONObject json) {

        // Checking if required paths exist, if they don't exist they are created
        Path triggerpath = Paths.get(triggerPath);
        Path activepath = Paths.get(activeTriggerPath);
        Path inactivepath = Paths.get(inactiveTriggerPath);
        if (Files.notExists(triggerpath)) {
            new File(triggerPath).mkdir();
        }
        if (Files.notExists(activepath)) {
            new File(activeTriggerPath).mkdir();
        }
        if (Files.notExists(inactivepath)) {
            new File(inactiveTriggerPath).mkdir();
        }

        // Writing the json file to path
        try (FileWriter file = new FileWriter(path)) {
            file.write(json.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Gets the parameters from a dataset when source is chosen in the Front-End
    @RequestMapping(path="/parametersget", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static String getParameters() throws IOException, NullPointerException {
        NetcdfFile hdf = NetcdfDataset.open(dataset.setDataset());

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

        hdf.close();

        return String.valueOf(phenomena);
    }

    // Gets the unit of a parameter when chosen in the Front-End
    @RequestMapping(path="/unitget", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static String getUnit(@RequestBody String payload) throws IOException {
        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);

        String parameter = triggerInfo.getString("parameter");

        NetcdfFile hdf = NetcdfDataset.open(dataset.setDataset());

        Group find = hdf.getRootGroup();
        String variable = hdf.findVariableByAttribute(find, "long_name", parameter).getName();
        String value = hdf.findAttValueIgnoreCase(hdf.findVariable(variable), "units", "units");

        JSONObject unit = new JSONObject();

        unit.put("unit", value);

        hdf.close();

        return String.valueOf(unit);
    }

    // Gets all active triggers that are in the active trigger path
    @RequestMapping(path="/gettriggers", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getTriggers() throws IOException {

        String trigger;
        ArrayList triggerInfoList = new ArrayList();
        File folder = new File(activeTriggerPath);
        File[] listOfFiles = folder.listFiles();
        files = new JSONArray();
        if (listOfFiles != null) {
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    files.add(listOfFiles[i].getName());
                }
            }
            for (int i = 0; i < files.size(); i++) {
                trigger = Tools.readFile(activeTriggerPath + files.get(i));
                triggerInfoList.add(trigger);
            }
        }
        return String.valueOf(triggerInfoList);
    }

    @RequestMapping(path= "/triggerdelete", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static void deleteTrigger(@RequestBody String payload) throws IOException {
        org.json.JSONObject trigger = new org.json.JSONObject(payload);

        File folder = new File(activeTriggerPath);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            org.json.JSONObject file = new org.json.JSONObject(Tools.readFile(String.valueOf(listOfFiles[i])));
            org.json.JSONObject phen = file.getJSONObject("phenomenon");
            if (phen.getString("UUID").equals(trigger.getString("uuid"))) {
                listOfFiles[i].delete();
            }
        }
    }
}
