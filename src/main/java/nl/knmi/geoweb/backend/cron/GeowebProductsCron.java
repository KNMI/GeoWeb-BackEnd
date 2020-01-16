package nl.knmi.geoweb.backend.cron;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeowebProductsCron {
  /* The maximum age of a product file in milliseconds (10 Days) */
  double PRODUCT_REMOVAL_AGE_MS = 10 * 24 * 60 * 60 * 1000;

  /* Start cleaning every hour */
  long PRODUCT_REMOVAL_TIMER_MS = 60 * 60 * 1000;

  @Value(value = "${geoweb.products.storeLocation}") String productstorelocation;

  public class CronJob extends TimerTask {


    void cleanTafs (double timeForRemoval) {
      /* Scan products folder TAFs and grab all files*/
      List<String> filesTAF = new ArrayList<>();
      try {
        log.debug("Retrieving TAFs from " + productstorelocation + "/tafs");
        File[] retrievedFiles = new File(productstorelocation + "/tafs").listFiles();
        long currentDate = new Date().getTime();
        for (int i = 0; i < retrievedFiles.length; i++){ 
          long lastModified = retrievedFiles[i].lastModified();
          if(retrievedFiles[i].isFile() && lastModified != 0 && (currentDate - lastModified > timeForRemoval)){
            try {
              String fileName = retrievedFiles[i].getName().toString();
              retrievedFiles[i].delete();
              filesTAF.add(fileName);
            } catch (Exception e) {
              log.error(e.getMessage());
            }
          }
        };
        log.debug("The following files have been deleted from the TAF products folder: " + filesTAF.toString());
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }

    public void cleanSigmets(double timeForRemoval) {
      /* Scan products folder SIGMETs and grab all files*/
      List<String> filesSigmet = new ArrayList<>();
      try {
        log.debug("Retrieving SIGMETs from " + productstorelocation + "/sigmets");
        File[] retrievedFiles = new File(productstorelocation + "/sigmets").listFiles();
        long currentDate = new Date().getTime();
        for (int i = 0; i < retrievedFiles.length; i++){ 
          long lastModified = retrievedFiles[i].lastModified();
          if(retrievedFiles[i].isFile() && lastModified != 0 && (currentDate - lastModified > timeForRemoval)){
            try {
              String fileName = retrievedFiles[i].getName().toString();
              retrievedFiles[i].delete();
              filesSigmet.add(fileName);
            } catch (Exception e) {
              log.error(e.getMessage());
            }
          }
        };
        log.debug("The following files have been deleted from the SIGMET products folder: " + filesSigmet.toString());
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }
  
    public void cleanAirmets(double timeForRemoval) {
      /* Scan products folder AIRMETs and grab all files*/
      List<String> filesAirmet = new ArrayList<>();
      try {
        log.debug("Retrieving AIRMETs from " + productstorelocation + "/airmets");
        File[] retrievedFiles = new File(productstorelocation + "/airmets").listFiles();
        long currentDate = new Date().getTime();
        for (int i = 0; i < retrievedFiles.length; i++){
          long lastModified = retrievedFiles[i].lastModified();
          if(retrievedFiles[i].isFile() && lastModified != 0 && (currentDate - lastModified > timeForRemoval)){
            try {
              String fileName = retrievedFiles[i].getName().toString();
              retrievedFiles[i].delete();
              filesAirmet.add(fileName);
            } catch (Exception e) {
              log.error(e.getMessage());
            }
          }
        };
        log.debug("The following files have been deleted from the AIRMET products folder: " + filesAirmet.toString());
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }

    public void cleanSharedPresets(double timeForRemoval) {
      /* Scan shared preset folder and grab all files*/
      List<String> filesSharedPresets = new ArrayList<>();
      try {
        log.debug("Retrieving shared presets from from " + productstorelocation + "/presets/shared");
        File[] retrievedFiles = new File(productstorelocation + "/presets/shared").listFiles();
        long currentDate = new Date().getTime();
        for (int i = 0; i < retrievedFiles.length; i++){
          long lastModified = retrievedFiles[i].lastModified();
          if(retrievedFiles[i].isFile() && lastModified != 0 && (currentDate - lastModified > timeForRemoval)){
            try {
              String fileName = retrievedFiles[i].getName().toString();
              retrievedFiles[i].delete();
              filesSharedPresets.add(fileName);
            } catch (Exception e) {
              log.error(e.getMessage());
            }
          }
        };
        log.debug("The following files have been deleted from the shared presets folder: " + filesSharedPresets.toString());
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    }
  
    @Override
    public void run() {
      /* The maximum age of a product file in milliseconds (10 Days) */
      double timeForRemoval = PRODUCT_REMOVAL_AGE_MS;
      
      /* Scan products folder TAFs and grab all files*/
      cleanTafs (timeForRemoval);

      /* Scan products folder SIGMETs and grab all files*/
      cleanSigmets (timeForRemoval);

      /* Scan products folder AIRMETs and grab all files*/
      cleanAirmets (timeForRemoval);

      /* Scan shared preset folder and grab all files*/
      cleanSharedPresets(timeForRemoval);
    }
  }
  GeowebProductsCron () {
    /* Start cleaning every hour */
    new Timer().scheduleAtFixedRate(new CronJob(), 10000, PRODUCT_REMOVAL_TIMER_MS);
  }
}