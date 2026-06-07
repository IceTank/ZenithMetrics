package org.icetank.metric.metrics;


import io.prometheus.metrics.core.metrics.CounterWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;

public class WorldInfo implements Registerable {
    @Override
    public void register(PrometheusRegistry registry) {
        CounterWithCallback.builder()
                .name("zenith_world_chunk_load_events_total")
                .help("Total number of chunk load events received for the world")
                .callback(callback -> callback.call(getModule().chunkLoadsAll))
                .register(registry);
    }
}
