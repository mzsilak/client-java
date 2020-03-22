package eu.arrowhead.demo.dto;

public class RegisterResponseDTO {

    private boolean success;

    public RegisterResponseDTO() {
    }

    public RegisterResponseDTO(boolean success) {
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
