package org.icetank.metric;


import io.prometheus.metrics.model.registry.PrometheusRegistry;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public interface Registerable {
    void register(PrometheusRegistry registry);
}
