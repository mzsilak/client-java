package eu.arrowhead.common.dto.shared;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.io.Serializable;

@JsonInclude(Include.NON_NULL)
public class OnboardingWithNameRequestDTO implements Serializable {

    //=================================================================================================
    // members

    private static final long serialVersionUID = 1L;

    private CertificateCreationRequestDTO creationRequestDTO;

    //=================================================================================================
    // constructors

    public OnboardingWithNameRequestDTO() {
    }

    public OnboardingWithNameRequestDTO(final String commonName) {
        this.creationRequestDTO = new CertificateCreationRequestDTO(commonName);
    }

    public OnboardingWithNameRequestDTO(final String commonName, final String publicKey, final String privateKey) {
        this.creationRequestDTO = new CertificateCreationRequestDTO(commonName, publicKey, privateKey);
    }

    public OnboardingWithNameRequestDTO(CertificateCreationRequestDTO creationRequestDTO) {
        this.creationRequestDTO = creationRequestDTO;
    }

    //=================================================================================================
    // methods
    //-------------------------------------------------------------------------------------------------

    public CertificateCreationRequestDTO getCreationRequestDTO() {
        return creationRequestDTO;
    }

    public void setCreationRequestDTO(CertificateCreationRequestDTO creationRequestDTO) {
        this.creationRequestDTO = creationRequestDTO;
    }
}