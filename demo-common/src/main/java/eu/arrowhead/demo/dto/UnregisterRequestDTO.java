package eu.arrowhead.demo.dto;

import java.io.Serializable;

public class UnregisterRequestDTO implements Serializable {

    private String rfid;

    public UnregisterRequestDTO() {
        super();
    }

    public UnregisterRequestDTO(String rfid) {
        this.rfid = rfid;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }
}
