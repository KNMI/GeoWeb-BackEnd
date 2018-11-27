package nl.knmi.geoweb.backend.triggers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ScheduledTasks {

    TriggerTest triggerTest;
    private String oldurl;

    {
        try {
            oldurl = triggerTest.setDataset();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Autowired
    WebSocketListener listener;

    // Checks every 1 minute (60000 ms) if new dataset is available
    @Scheduled(fixedRate = 60000)
    public void reportNotifications() throws IOException {

        String url = triggerTest.setDataset();

        // If new dataset is available send message to client
        if (didItChange(url) == true) {
            System.out.println(url);
            listener.pushSystemStatusToWebSocket("Notifications");
        }

    }

    private Boolean didItChange(String url) throws IOException {
        if (!oldurl.equals(url)){
            oldurl = triggerTest.setDataset();
            return true;
        } else {
            return false;
        }
    }
}