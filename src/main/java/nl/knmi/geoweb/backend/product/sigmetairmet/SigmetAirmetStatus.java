package nl.knmi.geoweb.backend.product.sigmetairmet;

import lombok.Getter;
import nl.knmi.adaguc.tools.Debug;

@Getter
public enum SigmetAirmetStatus {
    concept("concept"), canceled("canceled"), published("published");//, test("test"); TODO: Check, should be in Type now.
    private String status;
    private SigmetAirmetStatus (String status) {
        this.status = status;
    }
    public static SigmetAirmetStatus Status(String status){
        Debug.println("SIGMET/AIRMET status: " + status);

        for (SigmetAirmetStatus sstatus: SigmetAirmetStatus.values()) {
            if (status.equals(sstatus.toString())){
                return sstatus;
            }
        }
        return null;
    }

}
