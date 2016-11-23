package run.idle;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteSystemProperties;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;

/**
 * Start an embedded jetty server and handle all requests with IgniteHandler
 */
public class Main {
    public static void main(String args[]) throws Exception {
        System.setProperty(IgniteSystemProperties.IGNITE_NO_ASCII, "true");
        // Connect to the cluster based on Docker config options
        Ignite ignite = Ignition.start(new DockerIgniteConfig());

        // Initialize a cache with the given name if requested
        String cacheName = System.getProperty("CACHE_NAME");
        if (cacheName != null) {
            CacheConfiguration<Object,Object> conf = new CacheConfiguration<>(cacheName)
                    .setCacheMode(CacheMode.REPLICATED)
                    .setEvictSynchronized(true);
            ignite.getOrCreateCache(conf);
        }
    }
}
