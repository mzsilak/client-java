package eu.arrowhead.demo.dto;

import java.io.Serializable;

public class ChargeResponseDTO implements Serializable {

    private boolean success;

    public ChargeResponseDTO() {
    }

    public ChargeResponseDTO(boolean success) {
        super();
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
