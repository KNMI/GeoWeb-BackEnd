package nl.knmi.geoweb.backend.triggers;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

public class ActiveTriggers {

    TriggerTest triggerTest;

    @Autowired
    WebSocketListener listener;

    // Checks every half a second (500 ms) if new file is added to active trigger path
    public void reportActiveTriggers() {
//        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~MONITOR STARTED~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
//        File path = FileUtils.getFile(triggerTest.activeTriggerPath);
//        FileAlterationObserver observer = new FileAlterationObserver(path);
//
//        observer.addListener(new FileAlterationListenerAdaptor() {
//
//            @Override
//            public void onFileCreate(File file) {
//                System.out.println("Created: " + file.getName());
//                listener.pushSystemStatusToWebSocket("Active Triggers");
//            }
//
//            @Override
//            public void onFileDelete(File file) {
//                System.out.println("Deleted: " + file.getName());
//                listener.pushSystemStatusToWebSocket("Active Triggers");
//            }
//
//            @Override
//            public void onFileChange(File file) {
//                System.out.println("Changed: " + file.getName());
//                listener.pushSystemStatusToWebSocket("Active Triggers");
//            }
//
//        });
//
//        FileAlterationMonitor monitor = new FileAlterationMonitor(500, observer);
//        try {
//            monitor.start();
//            System.out.println("***Monitoring***");
//        } catch(IOException e) {
//            System.out.println(e.getMessage());
//        } catch(InterruptedException e) {
//            System.out.println(e.getMessage());
//        } catch (Exception e) {
//            System.out.println(e.getMessage());
//        }
    }
}
