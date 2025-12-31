package org.icetank.metric.metrics;


import com.zenith.Proxy;
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
    private static int highestEntityIdGame = 0;
    private static int highestEntityIdQueue = 0;
    private static final String CONTEXT_GAME = "game";
    private static final String CONTEXT_QUEUE = "queue";
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("zenith_entities_current")
                .help("Number of items dropped in the world")
                .labelNames("type", "context")
                .callback(callback -> {
                    String context = Proxy.getInstance().isInQueue() ? CONTEXT_QUEUE : CONTEXT_GAME;
                    Map<String, Integer> entityCountMap = CACHE.getEntityCache().getEntities().values().stream()
                            .collect(groupingBy(e -> {
                                var entityData = ENTITY_DATA.getEntityData(e.getEntityType());
                                return entityData != null ? entityData.name() : "unknown";
                            }, reducing(0, e -> 1, Integer::sum)));
                    for (Map.Entry<String, Integer> entry : entityCountMap.entrySet()) {
                        callback.call(entry.getValue(), entry.getKey(), context);
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
                .callback(callback -> {
                    callback.call(highestEntityIdGame, CONTEXT_GAME);
                    callback.call(highestEntityIdQueue, CONTEXT_QUEUE);
                })
                .labelNames("context")
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
        if (Proxy.getInstance().isInQueue()) {
            if (entityId > highestEntityIdQueue) {
                highestEntityIdQueue = entityId;
                return;
            }
            if (entityId < 0 && highestEntityIdQueue > 0) {
                highestEntityIdQueue = entityId;
            }
        } else {
            if (entityId > highestEntityIdGame) {
                highestEntityIdGame = entityId;
                return;
            }
            if (entityId < 0 && highestEntityIdGame > 0) {
                highestEntityIdGame = entityId;
            }
        }
    }
}
