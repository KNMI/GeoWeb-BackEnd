package nl.knmi.geoweb.backend.product.airmet;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import nl.knmi.adaguc.tools.Debug;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.sigmetairmet.SigmetAirmetStatus;

@Component
public class AirmetStore {

	private String directory;

	@Autowired
	@Qualifier("airmetObjectMapper")
	private ObjectMapper airmetObjectMapper;

	public ObjectMapper getOM(){ return airmetObjectMapper;}

	public AirmetStore(@Value(value = "${geoweb.products.storeLocation}") String productstorelocation) throws IOException {
		this.setLocation(productstorelocation);
	}

	public void setLocation(String productstorelocation) throws IOException {
		String dir = productstorelocation + "/airmets";
		Debug.println("airmet STORE at " + dir);
		File f = new File(dir);
		if(f.exists() == false){
			Tools.mksubdirs(f.getAbsolutePath());
			Debug.println("Creating airmet store at ["+f.getAbsolutePath()+"]");		}
		if(f.isDirectory() == false){
			Debug.errprintln("airmet directory location is not a directory");
			throw new NotDirectoryException("airmet directory location is not a directorty");
		}

		this.directory=dir;
	}

	public void storeAirmet(ObjectMapper om, Airmet airmet) {
		String fn=String.format("%s/airmet_%s.json", this.directory, airmet.getUuid());
		airmet.serializeAirmet(om, fn);
	}

	public void storeAirmet(Airmet airmet) {
		String fn=String.format("%s/airmet_%s.json", this.directory, airmet.getUuid());
		airmet.serializeAirmet(airmetObjectMapper, fn);
	}

	public synchronized int getNextSequence(Airmet airmetToPublish) {
		// Day zero means all airmets of today since midnight UTC
		Airmet[] allairmets = getPublishedAirmetsOnDay(airmetToPublish);
		Airmet[] airmets = (Airmet[]) Arrays.stream(allairmets).filter(
					x -> true).toArray(Airmet[]::new);

		int seq = 1;
		if (airmets.length > 0){
			Arrays.sort(airmets, (rhs, lhs) -> rhs.getSequence() < lhs.getSequence() ? 1 :
                    (rhs.getSequence() == lhs.getSequence() ? 0 : -1));
			seq = airmets[0].getSequence() + 1;
		}
		Debug.println("SEQUENCE NR: Created sequence number " + seq + " for airmet phenomenon " + airmetToPublish.getPhenomenon().toString());
		return seq;
	}

	private Airmet[] getPublishedAirmetsOnDay(Airmet airmetToPublish) {
		Airmet[] airmets = getAirmets(false, SigmetAirmetStatus.published);
		OffsetDateTime offsetSinceMidnight = airmetToPublish.getIssuedate().withHour(0).withMinute(1).withSecond(0).withNano(0);

		return Arrays.stream(airmets).filter(airmet -> (
				airmet.getValiddate().isAfter(offsetSinceMidnight) ||
				airmet.getValiddate().isEqual(offsetSinceMidnight)
				)).toArray(Airmet[]::new);
	}

	public Airmet[] __getPublishedAirmetsSinceDay (int daysOffset) {
		Airmet[] airmets = getAirmets(false, SigmetAirmetStatus.published);
		OffsetDateTime offset = OffsetDateTime.now(ZoneId.of("Z")).minusDays(daysOffset);
		OffsetDateTime offsetSinceMidnight = offset.withHour(0).withMinute(1).withSecond(0).withNano(0);

		return Arrays.stream(airmets).filter(airmet -> (
				airmet.getValiddate().isAfter(offsetSinceMidnight) ||
				airmet.getValiddate().isEqual(offsetSinceMidnight)
				)).toArray(Airmet[]::new);
	}

	public Airmet[] getAirmets(boolean selectActive, SigmetAirmetStatus selectStatus) {
		Comparator<Airmet> comp = new Comparator<Airmet>() {
			public int compare(Airmet lhs, Airmet rhs) {
				if (rhs.getIssuedate() != null && lhs.getIssuedate() != null)
					return lhs.getIssuedate().compareTo(rhs.getIssuedate());
				else 
					return lhs.getValiddate().compareTo(rhs.getValiddate());
			}
		};
		//Scan directory for airmets
		File dir=new File(directory);
		File[] files=dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !name.contains("..") && name.contains("airmet_")&&name.endsWith(".json");
			}
		});

		OffsetDateTime now= OffsetDateTime.now(ZoneId.of("Z"));

		if (files!=null) {
			List<Airmet> airmets=new ArrayList<>();
			for (File f: files) {
				Airmet sm;
				try {
					sm = Airmet.getAirmetFromFile(airmetObjectMapper, f);
					if (selectActive) {
//						Debug.println(sm.getStatus()+" "+now+" "+sm.getValiddate()+" "+sm.getValiddate_end());
						if ((sm.getStatus()==SigmetAirmetStatus.published)&&
								(sm.getValiddate_end().isAfter(now))) {
							airmets.add(sm);
						}
					}else if (selectStatus != null) {
						if (selectStatus==SigmetAirmetStatus.canceled) {
							if (((sm.getStatus()==SigmetAirmetStatus.published)&&sm.getValiddate_end().isBefore(now))||(sm.getStatus()==SigmetAirmetStatus.canceled)) {
								airmets.add(sm);
							}
						} else {
							if (sm.getStatus()==selectStatus) {
								airmets.add(sm);
							}
						}
					} else {
						airmets.add(sm);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			airmets.sort(comp);
			return airmets.toArray(new Airmet[0]);
		}
		return new Airmet[0];
	}

	public Airmet getByUuid(String uuid) {
		for (Airmet airmet: getAirmets(false, null)) {
			if (uuid.equals(airmet.getUuid())){
				return airmet;
			}
		}
		return null;
	}

	public boolean deleteAirmetByUuid(String uuid) {
		String fn=String.format("%s/airmet_%s.json", this.directory, uuid);
		return Tools.rm(fn);
	}

	public boolean isPublished(String uuid) {
		Airmet airmet=getByUuid(uuid);
		if (airmet!=null) {
			return (airmet.getStatus()== SigmetAirmetStatus.published);
		}
		return false;
	}

	public boolean isCanceled(String uuid) {
		Airmet airmet=getByUuid(uuid);
		if (airmet!=null) {
			return (airmet.getStatus()==SigmetAirmetStatus.canceled);
		}
		return false;
	}
}
