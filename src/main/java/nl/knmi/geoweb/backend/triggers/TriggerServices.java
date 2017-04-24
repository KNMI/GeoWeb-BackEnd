package nl.knmi.geoweb.backend.triggers;

import java.io.FileNotFoundException;
import java.nio.file.NotDirectoryException;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/triggers") 
public class TriggerServices {
	static TriggerStore store=null;	
	TriggerServices () throws NotDirectoryException {
		store = new TriggerStore("/tmp/triggers");
	}
	
	@RequestMapping("/gettriggers")
	public List<Trigger> getTriggers(@RequestParam("startdate")@DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm:ss'Z'") Date startDate, 
			                  @RequestParam("duration")Integer duration) {
		return store.getLastTriggers(startDate, duration);
	}

    @RequestMapping("/newtrigger")
    public String newTrigger() {
    	Trigger trig=new Trigger();
    	try {
			store.storeTrigger(trig);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return "stored trigger "+trig.getUuid();
    }
}
