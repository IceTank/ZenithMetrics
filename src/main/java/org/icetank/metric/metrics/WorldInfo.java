package org.icetank.metric.metrics;


import com.zenith.cache.data.entity.Entity;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.icetank.metric.Registerable;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.Map;

import static com.zenith.Globals.CACHE;

/*
 * @author IceTank
 * @since 08.11.2025
 */
public class WorldInfo implements Registerable {
    @Override
    public void register(PrometheusRegistry registry) {
        GaugeWithCallback.builder()
                .name("world_entity_count")
                .help("Number of items dropped in the world")
                .labelNames("type")
                .callback(callback -> {
                    Map<Integer, Entity> entities = CACHE.getEntityCache().getEntities();
                    callback.call(entities.size(), "all");
                    callback.call(entities.values().stream().filter(e -> e.getEntityType() == EntityType.PLAYER).count(), "players");
                    callback.call(entities.values().stream().filter(e -> e.getEntityType() == EntityType.ITEM).count(), "items");
                })
                .register(registry);
    }
}
