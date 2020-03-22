package eu.arrowhead.demo.dto;

import java.io.Serializable;

public class ChargeRequestDTO implements Serializable {

    private String rfid;

    public ChargeRequestDTO() {
        super();
    }

    public ChargeRequestDTO(String rfid) {
        this.rfid = rfid;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }
}
