package eu.arrowhead.demo.dto;

import java.io.Serializable;

public class PlcResponseDTO implements Serializable {

    private String plcAddress;

    public PlcResponseDTO() {
        super();
    }

    public PlcResponseDTO(String plcAddress) {
        this.plcAddress = plcAddress;
    }

    public String getPlcAddress() {
        return plcAddress;
    }

    public void setPlcAddress(String plcAddress) {
        this.plcAddress = plcAddress;
    }
}
