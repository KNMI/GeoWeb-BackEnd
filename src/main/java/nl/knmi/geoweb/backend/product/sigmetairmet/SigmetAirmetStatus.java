package nl.knmi.geoweb.backend.product.sigmetairmet;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public enum SigmetAirmetStatus {
    concept("concept"), canceled("canceled"), published("published");//, test("test"); TODO: Check, should be in Type now.
    private String status;
    private SigmetAirmetStatus (String status) {
        this.status = status;
    }
    public static SigmetAirmetStatus Status(String status){
        log.debug("SIGMET/AIRMET status: " + status);

        for (SigmetAirmetStatus sstatus: SigmetAirmetStatus.values()) {
            if (status.equals(sstatus.toString())){
                return sstatus;
            }
        }
        return null;
    }

}
