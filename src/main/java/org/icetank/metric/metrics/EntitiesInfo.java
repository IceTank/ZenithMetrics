package org.icetank.metric.metrics;


import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.icetank.metric.Registerable;

import java.util.ArrayList;
import java.util.List;
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
    private static Counter entityCounter;
    private static final List<Integer> uniqueEntityIds = new ArrayList<>();
    private static int highestEntityId = 0;
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("zenith_entities_current")
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
        entityCounter = Counter.builder()
                .name("zenith_entities_total")
                .help("Total number of entities created in the world")
                .labelNames("type")
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_entities_highest_id")
                .help("Highest entity ID encountered")
                .callback(callback -> callback.call(highestEntityId))
                .register(registry);
    }

    public static void incrementEntityCounter(EntityType entityType, int entityId) {
        if (uniqueEntityIds.contains(entityId)) {
            return;
        }
        uniqueEntityIds.add(entityId);
        while (uniqueEntityIds.size() > 1000) {
            uniqueEntityIds.removeFirst();
        }
        if (entityCounter == null) {
            return;
        }

        entityCounter.labelValues(ENTITY_DATA.getEntityData(entityType).name()).inc();
    }

    public static void recordHighestEntityId(int entityId) {
        if (entityId > highestEntityId) {
            highestEntityId = entityId;
            return;
        }
        if (entityId < 0 && highestEntityId > 0) {
            highestEntityId = entityId;
        }
    }
}
