package nl.knmi.geoweb.backend.triggers;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

public class Dataset {

    // Sets the dataset to use with the date in the name of the dataset
    public static String setDataset() throws IOException {
        String url;

        int year = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.YEAR);
        int month = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.MONTH) + 1;
        int day = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.DAY_OF_MONTH);
        int hour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY);
        int minutes = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.MINUTE)/10;

        if (minutes == 0) {
            hour = hour -1;
        }

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
            minutes = 5;
            if (hours.equals("00")) {
                hours = "23";
            }
        }

        url = "http://birdexp07.knmi.nl/geoweb/data/OBS/kmds_alle_stations_10001_" + year + months + days + hours + minutes + "0.nc";

        NetcdfFile hdf;

        try{
            hdf = NetcdfDataset.open(url);
        } catch(FileNotFoundException e) {
            minutes = minutes - 1;
            url = "http://birdexp07.knmi.nl/geoweb/data/OBS/kmds_alle_stations_10001_" + year + months + days + hours + minutes + "0.nc";
            hdf = NetcdfDataset.open(url);
        }

        hdf.close();

        return url;
    }
}
