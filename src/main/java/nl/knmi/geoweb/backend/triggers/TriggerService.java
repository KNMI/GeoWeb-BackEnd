package nl.knmi.geoweb.backend.triggers;

import nl.knmi.adaguc.tools.Tools;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.locationtech.jts.util.Debug;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

import javax.servlet.http.HttpServlet;
import java.io.*;
import java.io.FileWriter;
import java.util.*;

@RestController
@RequestMapping("/triggers")
public class TriggerService extends HttpServlet {

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

    private static Dataset dataset;

    @Autowired
    WebSocketListener listener;

    // Calculating the actual trigger with the values of active triggers over the values in the latest dataset and writes it to a json file in the trigger path
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

        // Reading the latest dataset
        NetcdfFile hdf = NetcdfDataset.open(dataset.setDataset());
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
                        createLocationJSONObject(x);
                        jsoncreated = true;
                    }
                }
            } else if (operator.equals("lower")) {
                for (int x = 0; x < station.getSize(); x++) {
                    if (data.getDouble(x) <= (double) limit && data.getDouble(x) >= -100) {
                        printed = true;
                        createLocationJSONObject(x);
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

                    // ...writing the calculated trigger json object to a file
                    writeJsonFile(activetriggerjsonpath, json);
                }
            }
        }
        hdf.close();
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
        locarray.add(locations);
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

        JSONObject phenomenon = new JSONObject();
        phenomenon.put("parameter", variable);
        phenomenon.put("long_name", par);
        phenomenon.put("operator", operator);
        phenomenon.put("limit", limit);
        phenomenon.put("unit", unit);
        phenomenon.put("source", source);

        // Path + name where the trigger will be saved as a json file
        triggerjsonpath = activeTriggerPath + "trigger_" + UUID.randomUUID() + ".json";

        json.put("phenomenon", phenomenon);

        // Writing the JSON file
        writeJsonFile(triggerjsonpath, json);

        hdf.close();
    }

    // Writing the JSON file with a try catch
    private static void writeJsonFile(String path, JSONObject json) {
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
    private String getTriggers() throws Exception {

        reportActiveTriggers();

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

    // Checks if new file is added to active trigger path
    public void reportActiveTriggers() throws Exception {
        File path = FileUtils.getFile(activeTriggerPath);
        FileAlterationObserver observer = new FileAlterationObserver(path);

        FileAlterationMonitor monitor = new FileAlterationMonitor(500, observer);

        observer.addListener(new FileAlterationListenerAdaptor() {

            @Override
            public void onFileCreate(File file) {
                System.out.println("Created: " + file.getName());
                listener.pushMessageToWebSocket("Active Triggers");
                try {
                    monitor.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFileDelete(File file) {
                System.out.println("Deleted: " + file.getName());
                listener.pushMessageToWebSocket("Active Triggers");
                try {
                    monitor.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFileChange(File file) {
                System.out.println("Changed: " + file.getName());
                listener.pushMessageToWebSocket("Active Triggers");
                try {
                    monitor.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        try {
            monitor.start();
            Thread.sleep(500);
        } catch(IOException e) {
            System.out.println(e.getMessage());
            monitor.stop();
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
            monitor.stop();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            monitor.stop();
        }
    }
}
