package eu.arrowhead.demo.dto;

import java.io.Serializable;

public class RegisterRequestDTO implements Serializable {

    private String rfid;

    public RegisterRequestDTO() {
        super();
    }

    public RegisterRequestDTO(String rfid) {
        this.rfid = rfid;
    }

    public String getRfid() {
        return rfid;
    }

    public void setRfid(String rfid) {
        this.rfid = rfid;
    }
}
