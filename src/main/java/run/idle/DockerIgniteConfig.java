package run.idle;

import com.google.common.io.BaseEncoding;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.log4j.Logger;

import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 */
public class DockerIgniteConfig extends IgniteConfiguration {

    private static final Logger LOG = Logger.getLogger(DockerIgniteConfig.class);

    private static final int PORT = 57101;

    public DockerIgniteConfig() {
        TcpCommunicationSpi communicationSpi = new TcpCommunicationSpi();
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalPort(PORT);

        String network = System.getProperty("NETWORK_PREFIX");
        if (network != null) {
                List<String> all = getNetworkAddresses();
                List<String> match = all.stream()
                        .filter(x -> x.startsWith(network))
                        .collect(Collectors.toList());
                Collections.reverse(match);

                if (match.isEmpty()) {
                    throw new RuntimeException("No matching addresses found for NETWORK_PREFIX=" + network + ", ADDRS=" + all);
                } else {
                    LOG.debug("Picking first of matching addresses: " + match);
                    String addr = match.get(0);
                    LOG.info("Set LOCAL_ADDRESS=" + addr);
                    communicationSpi.setLocalAddress(addr);
                    discoverySpi.setLocalAddress(addr);
                }
        } else {
            LOG.warn("No -DNETWORK_PREFIX is set, so localAddress will be auto-discovered");
        }

        // Load the dns address, probably tasks.servname which resolves to the list of host addresses
        String dns = System.getProperty("CLUSTER_DNS");
        if (dns != null) {
            try {
                List<String> hosts =
                        Arrays.asList(InetAddress.getAllByName(dns))
                        .stream()
                        .map(x -> x.getHostAddress() + ":" + PORT)
                        .collect(Collectors.toList());

                LOG.info("Set HOSTS=" + hosts);
                TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);
                ipFinder.setAddresses(hosts);
                discoverySpi.setIpFinder(ipFinder);
            } catch (UnknownHostException e) {
                throw new RuntimeException("Failed to resolve DNS '" + dns + "'", e);
            }
        } else {
            LOG.warn("No -DCLUSTER_DNS is set, so multicast will be used instead");
            discoverySpi.setIpFinder(new TcpDiscoveryMulticastIpFinder());
        }

        setCommunicationSpi(communicationSpi);
        setDiscoverySpi(discoverySpi);
    }

    /**
     * Gets a list of all network addresses (on all interfaces)
     * For each interface the associated addresses are consistently sorted in ascending order
     */
    private static List<String> getNetworkAddresses() {
        try {
            BaseEncoding hex = BaseEncoding.base16();
            List<String> all = new ArrayList<>();
            for(NetworkInterface itf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                all.addAll(Collections.list(itf.getInetAddresses())
                        .stream()
                        .filter(x -> x instanceof Inet4Address)
                        .map(InetAddress::getAddress)
                        .map(hex::encode)
                        .sorted() // sort using hex to avoid string ordering problems (.12 before .2)
                        .map(hex::decode)
                        .map(b -> String.format("%d.%d.%d.%d", // convert back to standard decimal IP format
                                0xff & b[0], 0xff & b[1],
                                0xff & b[2], 0xff & b[3]))
                        .collect(Collectors.toList()));
            }
            LOG.trace("Available network addresses: " + all);
            return all;
        } catch (SocketException e) {
            throw new RuntimeException("Failed to load network interfaces", e);
        }
    }
}
