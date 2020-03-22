package eu.arrowhead.demo.dto;

import java.io.Serializable;

public class RfidResponseDTO implements Serializable {

    private String rfid;

    public RfidResponseDTO() {
        super();
    }

    public RfidResponseDTO(String rfid) {
        this.rfid = rfid;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }
}
