package eu.arrowhead.client.common.model;

import java.time.LocalDateTime;

public class DeviceRegistryEntry {
    private Long id;
    private ArrowheadDevice providedDevice;
    private String macAddress;
    private LocalDateTime endOfValidity;

    public DeviceRegistryEntry() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ArrowheadDevice getProvidedDevice() {
        return providedDevice;
    }

    public void setProvidedDevice(ArrowheadDevice providedDevice) {
        this.providedDevice = providedDevice;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public LocalDateTime getEndOfValidity() {
        return endOfValidity;
    }

    public void setEndOfValidity(LocalDateTime endOfValidity) {
        this.endOfValidity = endOfValidity;
    }
}
