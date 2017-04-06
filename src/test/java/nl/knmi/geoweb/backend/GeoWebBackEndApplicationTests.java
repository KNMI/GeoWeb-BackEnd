package nl.knmi.geoweb.backend;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.Phenomenon;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetChange;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet.SigmetStatus;
import nl.knmi.geoweb.backend.product.sigmet.SigmetStore;
import tools.Debug;
import tools.Tools;




@RunWith(SpringRunner.class)
@SpringBootTest
public class GeoWebBackEndApplicationTests {
	@Rule
	public final ExpectedException exception = ExpectedException.none();
	
	public final String sigmetStoreLocation = "/tmp/junit/geowebbackendstore/";
	
	static String testGeoJson="{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[4.44963571205923,52.75852934878266],[1.4462013467168233,52.00458561642831],[5.342222631879865,50.69927379063084],[7.754619712476178,50.59854892065259],[8.731640530117685,52.3196364467871],[8.695454573908739,53.50720041878871],[6.847813968390116,54.08633053026368],[3.086939481359807,53.90252679590722]]]},\"properties\":{\"prop0\":\"value0\",\"prop1\":{\"this\":\"that\"}}}]}";

	public Sigmet createSigmet () throws Exception {
		Sigmet sm=new Sigmet("AMSTERDAM FIR", "EHAA", "EHDB", "abcd");
		sm.setPhenomenon(Phenomenon.getPhenomenon("OBSC_TS"));
		sm.setValiddate(new Date(117,2,13,16,0));
		sm.setChange(SigmetChange.NC);
		sm.setGeoFromString(testGeoJson);
		return sm;
	}
	
	public void validateSigmet (Sigmet sm) throws Exception {
		Debug.println("Testing createAndCheckSigmet");
		Debug.println(sm.getValiddate().toString());
		assertThat(sm.getPhenomenon().toString(), is("OBSC_TS"));
	}
	
	@Test 
	public void createAndValidateSigmet () throws Exception {
		Sigmet sm = createSigmet();
		validateSigmet(sm);
	}

	@Test
	public void createSigmetStoreAtEmptyDirCheckException () throws Exception {
		Tools.rmdir(sigmetStoreLocation);
		exception.expect(NotDirectoryException.class);
		new SigmetStore(sigmetStoreLocation);
	}
	
	public SigmetStore createNewStore() throws IOException {
		Tools.rmdir(sigmetStoreLocation);
		Tools.mksubdirs(sigmetStoreLocation);
		SigmetStore store=new SigmetStore(sigmetStoreLocation);
		Sigmet[] sigmets=store.getSigmets(false, SigmetStatus.PRODUCTION);
		assertThat(sigmets.length, is(0));
		return store;
	}
	
	@Test
	public void saveOneSigmet () throws Exception {
		SigmetStore store=createNewStore();
		Sigmet sm = createSigmet();
		store.storeSigmet(sm);
		assertThat(store.getSigmets(false, SigmetStatus.PRODUCTION).length, is(1));
	}
	
	@Test
	public void loadAndValidateSigmet () throws Exception {
		saveOneSigmet();
		SigmetStore storeLoad=new SigmetStore(sigmetStoreLocation);
		Sigmet[] sigmets=storeLoad.getSigmets(false, SigmetStatus.PRODUCTION);
		assertThat(sigmets.length, is(1));
		validateSigmet(sigmets[0]);
	}
}
