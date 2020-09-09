package eu.arrowhead.client.idaice;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.dto.shared.CertificateCreationRequestDTO;
import eu.arrowhead.common.dto.shared.DeviceRequestDTO;
import eu.arrowhead.common.dto.shared.SystemRequestDTO;
import eu.arrowhead.demo.utils.IpUtilities;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ArrowheadConfig {

    @Bean("ip")
    public String ipAddress() throws SocketException {
        return IpUtilities.getAddressString();
    }

    @Bean("mac")
    public String macAddress(@Qualifier("ip") final String ipAddress) throws SocketException, UnknownHostException {
        return IpUtilities.getMacAddress(ipAddress);
    }

    @Bean("validity")
    public String validity() {
        return Utilities.convertZonedDateTimeToUTCString(ZonedDateTime.now().plusDays(1));
    }

    @Bean
    public DeviceRequestDTO getDeviceRequest(@Value("${server.name}") final String commonName,
                                             @Qualifier("ip") final String ipAddress,
                                             @Qualifier("mac") final String macAddress) {
        return new DeviceRequestDTO(commonName, ipAddress, macAddress, null);
    }

    @Bean
    public SystemRequestDTO getSystemRequest(@Value("${server.name}") final String commonName,
                                             @Qualifier("ip") final String ipAddress,
                                             @Value("${server.port}") final int port) {
        return new SystemRequestDTO(commonName, ipAddress, port, null);
    }

    @Bean
    public CertificateCreationRequestDTO get(@Value("${server.name}") final String commonName) {
        return new CertificateCreationRequestDTO(commonName);
    }
}
