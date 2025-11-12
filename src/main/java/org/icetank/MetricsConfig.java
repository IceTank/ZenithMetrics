package org.icetank;

import java.util.HashMap;
import java.util.Map;

/**
 * Example configuration POJO.
 *
 * Configurations are saved and loaded to JSON files
 *
 * All fields should be public and mutable.
 *
 * Fields to static inner classes generate nested JSON objects.
 */
public class MetricsConfig {
    public boolean enabled = true;

    public int port = 9411; // Change to 0 to randomly assign port

    public ServiceDiscovery serviceDiscovery = new ServiceDiscovery();
    public static class ServiceDiscovery {
        public boolean enabled = false;
        public String accountName = "";
        public String host = "localhost";
        public int port = 9092;
        public String targetHost = "localhost";
        public Map<String, String> labels = new HashMap<>();
    }
}
