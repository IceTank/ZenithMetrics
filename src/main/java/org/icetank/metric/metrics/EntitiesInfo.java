package org.icetank.metric.metrics;


import com.zenith.mc.entity.EntityRegistry;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;

import java.util.Map;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.ENTITY_DATA;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public class EntitiesInfo implements Registerable {
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("zenith_total_entities")
                .help("Number of items dropped in the world")
                .labelNames("type")
                .callback(callback -> {
                    Map<String, Integer> entityCountMap = CACHE.getEntityCache().getEntities().values().stream()
                            .collect(groupingBy(e -> {
                                var entityData = ENTITY_DATA.getEntityData(e.getEntityType());
                                return entityData != null ? entityData.name() : "unknown";
                            }, reducing(0, e -> 1, Integer::sum)));
                    for (Map.Entry<String, Integer> entry : entityCountMap.entrySet()) {
                        callback.call(entry.getValue(), entry.getKey());
                    }
                })
                .register(registry);
    }
}
