package nl.knmi.geoweb.backend.traceability;

import lombok.extern.slf4j.Slf4j;
import nl.knmi.geoweb.backend.usermanagement.UserLogin;

@Slf4j(topic="Geoweb-Traceability")
public class ProductTraceability {

    public static void TraceProduct(String action, String product, String uuid, String location, String issueTime, String filename ){
        String userName = UserLogin.getUserName();
        userName = (userName == null) ? "No user" : userName;
        log.info("{} {} {} uuid: {} {} {} {}", userName, action, product, uuid, location, issueTime, filename);
    }
}    
