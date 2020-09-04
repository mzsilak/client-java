package eu.arrowhead.demo.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.text.View;

public class IpUtilities {

    private IpUtilities() {
    }

    /* If needed, this method can be used to get the IPv4 address of the host machine. Public point-to-point IP
  addresses are prioritized over private
    (site local) IP addresses */
    @SuppressWarnings("unused")
    public static String getAddressString() throws SocketException {
        return getInetAddress().getHostAddress();
    }

    private static InetAddress getInetAddress() throws SocketException {
        List<InetAddress> addresses = new ArrayList<>();

        Enumeration e = NetworkInterface.getNetworkInterfaces();
        while (e.hasMoreElements()) {
            NetworkInterface inf = (NetworkInterface) e.nextElement();
            Enumeration ee = inf.getInetAddresses();
            while (ee.hasMoreElements()) {
                addresses.add((InetAddress) ee.nextElement());
            }
        }

        addresses = addresses.stream().filter(current -> !current.getHostAddress().contains(":"))
                             .filter(current -> !current.isLoopbackAddress())
                             .filter(current -> !current.isMulticastAddress())
                             .filter(current -> !current.isLinkLocalAddress()).collect(Collectors.toList());
        if (addresses.isEmpty()) {
            throw new SocketException("No valid addresses left after filtering");
        }
        for (InetAddress address : addresses) {
            if (!address.isSiteLocalAddress()) {
                return address;
            }
        }
        return addresses.get(0);
    }

    public static String getMacAddress() throws SocketException {
        return getMacAddress(getInetAddress());
    }

    public static String getMacAddress(final InetAddress inetAddress) throws SocketException {
        NetworkInterface network = NetworkInterface.getByInetAddress(inetAddress);
        byte[] mac = network.getHardwareAddress();

        if (mac == null) {
            mac = new byte[]{0, 0, 0, 0, 0, 0};
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }

        return sb.toString();
    }

    public static String getMacAddress(final String address) throws SocketException, UnknownHostException {
        return getMacAddress(InetAddress.getByName(address));
    }
}
