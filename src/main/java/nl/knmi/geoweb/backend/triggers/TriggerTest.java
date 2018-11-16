package nl.knmi.geoweb.backend.triggers;

import HTTPClient.HTTPResponse;
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
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.rowset.serial.SerialException;
import java.io.*;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@RestController
@RequestMapping("/triggers")
public class TriggerTest extends HttpServlet {

    private static String name = null;
    private static String unit = null;
    private static String variable = null;
    public static String activetriggerjsonpath = null;
    public static String triggerjsonpath = null;
    public static String activeTriggerPath = "/nobackup/users/schouten/Triggers/ActiveTriggers/";
    public static String triggerPath = "/nobackup/users/schouten/Triggers";
    private static Array
            station = null,
            data = null,
            lat = null,
            lon = null,
            code = null;
    private static boolean printed = false;
    private static boolean jsoncreated = false;
    private boolean changed = false;
    private static JSONArray locarray = null;
    private static JSONArray files = null;

    @RequestMapping(path= "/triggercalculate", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static JSONObject calculateTrigger(@RequestBody String payload) throws IOException, InvalidRangeException, ParseException {

        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);
        System.out.println(payload);

        File folder = new File(activeTriggerPath);
        File[] listOfFiles = folder.listFiles();
        files = new JSONArray();
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                files.add(listOfFiles[i].getName());
            }
        }

        String path = triggerInfo.getString("serviceurl");
        NetcdfFile hdf = NetcdfDataset.open(path);
        station = hdf.readSection("stationname");

        JSONParser parser = new JSONParser();

        JSONObject json = new JSONObject();

        for(int i = 0; i < files.size(); i++) {
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

                activetriggerjsonpath = triggerPath + "trigger_" + LocalDateTime.now().format(formatter) + ".json";  // Path + name where the trigger will be saved as a json file

                json.put("locations", locarray);
                json.put("phenomenon", triggerFile);

                // Creating the json file with a try catch

                try (FileWriter file = new FileWriter(activetriggerjsonpath)) {
                    file.write(json.toJSONString());
                    file.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
//        String path = triggerInfo.getString("serviceurl");
//        Object par = phen.get("parameter");
//        Object operator = phen.get("operator");
//        Object limit = phen.get("limit");
//
//        NetcdfFile hdf = NetcdfDataset.open(path);
//
//        station = hdf.readSection("stationname");
//        Group find = hdf.getRootGroup();
//        String variable = hdf.findVariableByAttribute(find, "long_name", String.valueOf(par)).getName();
//        data = hdf.readSection(String.valueOf(variable));
//        lat = hdf.readSection("lat");
//        lon = hdf.readSection("lon");
//        code = hdf.readSection("station");
//
//        JSONObject json = new JSONObject();
//
//        locarray = new JSONArray();
//
//        if (operator.equals("higher")) {
//            for (int x = 0; x < station.getSize(); x++) {
//                if (data.getDouble(x) >= (double) limit) {
//                    printed = true;
//                    createJSONObject(x);
//                    jsoncreated = true;
//                }
//            }
//        } else if (operator.equals("lower")) {
//            for (int x = 0; x < station.getSize(); x++) {
//                if (data.getDouble(x) <= (double) limit && data.getDouble(x) >= -100) {
//                    printed = true;
//                    createJSONObject(x);
//                    jsoncreated = true;
//                }
//            }
//        }
//        if (!printed) {
//            printed = true;
//        }
//
//        if (jsoncreated) {
//            // Setting a format of the date and time with only numbers (to put in the name of the trigger file)
//
//            DateTimeFormatter formatter = new DateTimeFormatterBuilder()
//                    .appendValue(ChronoField.YEAR, 4)
//                    .appendValue(ChronoField.MONTH_OF_YEAR, 2)
//                    .appendValue(ChronoField.DAY_OF_MONTH, 2)
//                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
//                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
//                    .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
//                    .appendValue(ChronoField.MILLI_OF_SECOND, 5)
//                    .toFormatter();
//
//            activetriggerjsonpath = triggerPath + "trigger_" + LocalDateTime.now().format(formatter) + ".json";  // Path + name where the trigger will be saved as a json file
//
//            json.put("locations", locarray);
//            json.put("phenomenon", triggerFile);
//
//            // Creating the json file with a try catch
//
//            try (FileWriter file = new FileWriter(activetriggerjsonpath)) {
//                file.write(json.toJSONString());
//                file.flush();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        return json;
    }

    @RequestMapping(path= "/triggercreate", method= RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public static void addTrigger(@RequestBody String payload) throws IOException {

        org.json.JSONObject triggerInfo = new org.json.JSONObject(payload);
        System.out.println(payload);

        String path = triggerInfo.getString("serviceurl");
        String par = triggerInfo.getString("parameter");
        String operator = triggerInfo.getString("operator");
        Double limit = triggerInfo.getDouble("limit");
        String source = triggerInfo.getString("source");

        NetcdfFile hdf = NetcdfDataset.open(path);

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

    @RequestMapping(path="/servlettest", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public void servletTest(HttpServletResponse response) throws IOException {
        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        PrintWriter writer = response.getWriter();

        File path = FileUtils.getFile("/nobackup/users/schouten/Triggers/ActiveTriggers");
        FileAlterationObserver observer = new FileAlterationObserver(path);

        observer.addListener(new FileAlterationListenerAdaptor(){

            @Override
            public void onFileCreate(File file) {
                changed = true;
            }

        });

        if(changed == true) {
            writer.write("data: A new file has been uploaded!\n\n");
            changed = false;
        }

        FileAlterationMonitor monitor = new FileAlterationMonitor(500, observer);

        try {
            monitor.start();
        } catch(IOException e) {
            System.out.println(e.getMessage());
        } catch(InterruptedException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        writer.close();
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
