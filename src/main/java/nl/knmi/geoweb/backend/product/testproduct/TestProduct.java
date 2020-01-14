package nl.knmi.geoweb.backend.product.testproduct;

import java.io.File;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.knmi.adaguc.tools.Tools;
import nl.knmi.geoweb.backend.product.GeoWebProduct;
import nl.knmi.geoweb.backend.product.IExportable;
import nl.knmi.geoweb.backend.product.ProductConverter;

@Slf4j
@JsonInclude(Include.NON_EMPTY)
@Getter
@Setter
public class TestProduct implements GeoWebProduct, IExportable<TestProduct> {
    String timeStamp;
    String userName;
    String backendURL;



    public TestProduct(TestProduct otherTestProduct) {
        this.timeStamp=otherTestProduct.getTimeStamp();
        this.userName=otherTestProduct.getUserName();
    }

    public TestProduct() {
	}

	@Override
    public String export(File path, ProductConverter<TestProduct> converter, ObjectMapper om) {
        try{
            OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Z"));
            String time = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            String jsonFileName = "TEST_PRODUCT_" + time;
            String jsonFilePath = path.getPath() + "/" + jsonFileName + ".json";
            log.trace("write test product file");
            Tools.writeFile(jsonFilePath, this.toJSON(om));
        }  catch (Exception e) {
            return "ERROR: "+e.getMessage();
        }
        return "OK";
    }

    private String toJSON(ObjectMapper om) throws JsonProcessingException{
        return om.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }
    
}
