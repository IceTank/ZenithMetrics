package org.icetank.metric;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.metrics.*;

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

    private void register(PrometheusRegistry registry) {
        if (REGISTERED.add(registry)) {
            new QueueStatus().register(registry);
            new GameInfo().register(registry);
            new EntitiesInfo().register(registry);
            new PlayerInfo().register(registry);
            new ItemDrops().register(registry);
            new WorldInfo().register(registry);
        }
    }
}
