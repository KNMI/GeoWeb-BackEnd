package nl.knmi.geoweb.backend.triggers;

import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
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