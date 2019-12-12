package nl.knmi.geoweb.iwxxm_2_1.converter;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.TestConfig;
import nl.knmi.geoweb.backend.aviation.FIRStore;
import nl.knmi.geoweb.backend.product.sigmet.Sigmet;
import nl.knmi.geoweb.backend.product.sigmet.converter.SigmetConverter;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TestConfig.class })
public class VASigmetToIWXXMTest {
	@Autowired
	@Qualifier("sigmetObjectMapper")
	private ObjectMapper sigmetObjectMapper;

	@Autowired
	private SigmetConverter sigmetConverter;

	@Value("classpath:nl/knmi/geoweb/iwxxm_2_1/converter/vasigmet.json")
	Resource vaSigmetResource1;

	@Value("classpath:nl/knmi/geoweb/iwxxm_2_1/converter/SIGMET_EHDB_2018-11-29T1230_20181129111546.json")
	Resource vaSigmetResource2;

	@Autowired
	FIRStore firStore;

	@Test
	public void TestConversion (){
		List<Resource> vaResources = Arrays.asList(vaSigmetResource1, vaSigmetResource2);
		vaResources.forEach(resource -> {
			Sigmet vaSigmet = null;
			try {
				vaSigmet = sigmetObjectMapper.readValue(resource.getInputStream(), Sigmet.class);
			} catch (Exception e) {
				log.error(e.getMessage());
			}

			String result = sigmetConverter.ToIWXXM_2_1(vaSigmet);
			log.debug(result);
			log.debug("TAC: " + vaSigmet.toTAC(firStore.lookup(vaSigmet.getFirname(), true)));
		});
		
	}
}
