package org.icetank.metric;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.metrics.PlayerInfo;
import org.icetank.metric.metrics.WorldInfo;
import org.icetank.metric.metrics.QueueStatus;
import org.icetank.metric.metrics.ServerInfo;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Metrics {
    private static final Set<PrometheusRegistry> REGISTERED = ConcurrentHashMap.newKeySet();
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        public void register() {
            register(PrometheusRegistry.defaultRegistry);
        }
        public void register(PrometheusRegistry registry) {
            new Metrics().register(registry);
        }
    }
    public void register() {
        register(PrometheusRegistry.defaultRegistry);
    }

    public void register(PrometheusRegistry registry) {
        if (REGISTERED.add(registry)) {
            new QueueStatus().register(registry);
            new ServerInfo().register(registry);
            new WorldInfo().register(registry);
            new PlayerInfo().register(registry);
        }
    }
}
