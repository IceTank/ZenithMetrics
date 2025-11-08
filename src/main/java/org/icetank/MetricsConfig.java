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

    public ServiceDiscovery serviceDiscovery = new ServiceDiscovery();
    public static class ServiceDiscovery {
        public String accountName = "";
        public String host = "http://localhost";
        public int port = 9092;
        public String targetHost = "localhost";
        public Map<String, String> labels = new HashMap<>();
    }
}
