package nl.knmi.geoweb.backend.triggers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/triggers") 
public class TriggerServices {
	TriggerStore triggerStore;	
	
	TriggerServices (final TriggerStore triggerStore) throws IOException {
		this.triggerStore = triggerStore;
	}
	
	@RequestMapping("/gettriggers")
	public List<Trigger> getTriggers(@RequestParam("startdate")@DateTimeFormat(pattern="yyyy-MM-dd'T'HH:mm:ss'Z'") Date startDate, 
			                  @RequestParam("duration")Integer duration) {
		List<Trigger>triggers=triggerStore.getLastTriggers(startDate, duration);
		for (Trigger trigger: triggers) {
			List<String>presets=new ArrayList<String>();
			for (int i=1; i<=3; i++) {
				presets.add(trigger.getPhenomenon().getSource()+"_preset_"+i);
			}
			trigger.setPresets(presets);
		}
		return triggers;
	}

    @RequestMapping(path="/addtrigger", method=RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<String> addTrigger(@RequestBody Trigger.TriggerTransport transport) {
    	Trigger trig=new Trigger(transport);
    	try {
			triggerStore.storeTrigger(trig);
	    	return ResponseEntity.status(HttpStatus.OK).body("OK");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FAIL");
    }
}