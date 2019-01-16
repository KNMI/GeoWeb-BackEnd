package nl.knmi.geoweb.backend.triggers;

import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;

@Component
@RequestMapping("/demotask")
public class ScheduledTasks {

    private TriggerService triggerService;
    private Dataset dataset;
    private String oldurl;

    {
        try {
            oldurl = dataset.setDataset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Autowired
    WebSocketListener listener;

    // Checks every 1 minute (60000 ms) if new dataset is available
    @Scheduled(fixedRate = 60000)
    public void reportNotifications() throws Exception {

        String url = dataset.setDataset();

        // If new dataset is available and if there are triggers to calculate, send message to client
        if (didItChange(url) == true) {
            System.out.println(url);
            JSONArray triggers = triggerService.calculateTrigger();
            if (triggers.size() > 0) {
                System.out.println(triggers);
                JSONObject json = new JSONObject();
                json.put("Notifications", triggers);
                listener.pushMessageToWebSocket(String.valueOf(json));
            }
        }

    }

    //For Demo Purposes
    @RequestMapping(path="/demo", method= RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void demoShow() throws ParseException, InvalidRangeException, IOException {
        System.out.println("TEST");
        JSONArray triggers = triggerService.calculateTrigger();
        System.out.println(triggers);
        JSONObject json = new JSONObject();
        json.put("Notifications", triggers);
        listener.pushMessageToWebSocket(String.valueOf(json));
    }

    // Check if new dataset is available
    private Boolean didItChange(String url) throws IOException {
        if (!oldurl.equals(url)){
            oldurl = dataset.setDataset();
            return true;
        } else {
            return false;
        }
    }
}