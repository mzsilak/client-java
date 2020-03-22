package eu.arrowhead.demo.dto;

public class UnregisterResponseDTO {

    private boolean success;

    public UnregisterResponseDTO() {
    }

    public UnregisterResponseDTO(boolean success) {
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
