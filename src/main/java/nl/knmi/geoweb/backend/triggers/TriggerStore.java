package nl.knmi.geoweb.backend.triggers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.triggers.model.Trigger;

@Component
public class TriggerStore {
	private static final Logger LOGGER = LoggerFactory.getLogger(TriggerStore.class);

	private String directory;

	public TriggerStore(@Value(value = "${productstorelocation}") String productstorelocation) throws IOException {
		String dir = productstorelocation + "/triggers";
		LOGGER.debug("TRIGGER STORE at {}", dir);
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			LOGGER.debug("Creating triggerdir at [{}]", f.getAbsolutePath());
		}
		if(f.isDirectory() == false){
			LOGGER.error("Trigger directory location is not a directory");
			throw new NotDirectoryException("Trigger directory location is not a directory");
		}
		this.directory=dir;
	}

	public List<Trigger> getLastTriggers(Date startDate, int duration) {
		List<Trigger> triggers=new ArrayList<Trigger>();
		Date endDate=new Date(startDate.getTime()+duration*1000);
		try (DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(directory),
				new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				if (entry.getFileName().toString().startsWith("trigger_")) {
					Trigger trig=loadTriggerFromFile(entry.toString());
					Date trigdt=trig.getTriggerdate();
					return !startDate.after(trigdt)&&endDate.after(trigdt);
				}
				return false;
			}
		})
				){
			for (Path path : files) {
				Trigger trig=this.loadTriggerFromFile(path.toString());
				triggers.add(trig);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return triggers;
	}

	public void storeTrigger(Trigger trigger) throws FileNotFoundException {
		String fn=this.directory+"/"+"trigger_"+UUID.randomUUID()+".json";
		ObjectMapper om =new ObjectMapper();
		String json="";
		try {
			json=om.writeValueAsString(trigger);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File f=null;

		f=new File(fn);
		try (PrintStream out = new PrintStream(new FileOutputStream(f))) {
			out.print(json);
		}	
	}


	public Trigger loadTriggerFromFile(String fn) throws IOException {
		String json=new String(Files.readAllBytes(Paths.get(fn)));
		return loadJsonTrigger(json);

	}
	public Trigger loadJsonTrigger(String json) {
		ObjectMapper om=new ObjectMapper();
		try {
			return om.readValue(json, Trigger.class);
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
