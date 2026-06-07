package org.icetank.metric;


import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.module.MetricsModule;

import static com.zenith.Globals.MODULE;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public interface Registerable {
    void register(PrometheusRegistry registry);
    default MetricsModule getModule() {
        return MODULE.get(MetricsModule.class);
    }
}
