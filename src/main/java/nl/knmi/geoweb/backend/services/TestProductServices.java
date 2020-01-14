package nl.knmi.geoweb.backend.services;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.datastore.ProductExporter;
import nl.knmi.geoweb.backend.product.testproduct.TestProduct;
import nl.knmi.geoweb.backend.product.testproduct.converter.TestProductConverter;
import nl.knmi.geoweb.backend.usermanagement.UserLogin;

@Slf4j
@RestController

@RequestMapping("/testproduct")
public class TestProductServices {

    @Autowired
    private ProductExporter<TestProduct> publishTestProductStore;
    
    @Autowired
    @Qualifier("testProductObjectMapper")
    private ObjectMapper testProductObjectMapper;

    @Autowired
    TestProductConverter testProductConverter;

    @RequestMapping(path = "", 
                    method = {RequestMethod.GET, RequestMethod.POST}, 
                    produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<JSONObject> createTestProduct(@Value("${client.backendURL}") String backendURL) {
        log.trace("createTestProduct");

        String currentTime = OffsetDateTime.now(ZoneId.of("Z")).toString(); 
        TestProduct testProduct = new TestProduct();
        testProduct.setTimeStamp(currentTime);
        testProduct.setUserName(UserLogin.getUserName());
        testProduct.setBackendURL(backendURL);

        try{
            String result = publishTestProductStore.export(testProduct, testProductConverter, testProductObjectMapper);
            if (result.equals("OK")) {
                JSONObject json = new JSONObject()
                        .put("succeeded", true)
                        .put("message", "Test Product published");
                return ResponseEntity.ok(json);
            } else {
                JSONObject json = new JSONObject()
                        .put("succeeded", false)
                        .put("message", "Test Product failed to publish (" + result + ")");
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(json);
            }
        }catch(Exception e){
            log.error(e.getMessage());
            try {
                JSONObject obj=new JSONObject();
                obj.put("error",e.getMessage());
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(obj);
            } catch (JSONException e1) {
            }
        }
        log.error("Unknown error");
        JSONObject obj=new JSONObject();
        try {
            obj.put("error", "Unknown error");
        } catch (JSONException e) {
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(obj);
    }
}
