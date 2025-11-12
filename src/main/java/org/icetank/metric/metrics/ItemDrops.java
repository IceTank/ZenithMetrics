package org.icetank.metric.metrics;

import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.icetank.metric.Registerable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zenith.Globals.CACHE;

public class ItemDrops implements Registerable {
    public static Gauge itemCounter;
    public static String STATUS_CREATED = "created";
    public static String STATUS_DELETED = "deleted";
    private static List<Integer> uniqueItemIds = new ArrayList<>();
    @Override
    public void register(PrometheusRegistry registry) {
        itemCounter = Gauge.builder()
                .name("zenith_items_changed")
                .labelNames("item", "status")
                .register(registry);
        GaugeWithCallback.builder()
                .name("zenith_total_items")
                .labelNames("item")
                .callback(callback -> {
                    Map<String, Integer> itemAmountMap = new HashMap<>();
                    CACHE.getEntityCache().getEntities().values().stream().filter(e -> e.getEntityType() == EntityType.ITEM).forEach(e -> {
                        ItemStack itemStack = e.getMetadataValue(8, MetadataTypes.ITEM, ItemStack.class);
                        if (itemStack == null) {
                            return;
                        }
                        ItemData itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
                        if (itemData == null) {
                            return;
                        }
                        String itemName = itemData.name();
                        itemAmountMap.merge(itemName, itemStack.getAmount(), Integer::sum);
                    });
                    for (Map.Entry<String, Integer> entry : itemAmountMap.entrySet()) {
                        callback.call(entry.getValue(), entry.getKey());
                    }
                })
                .register(registry);
    }

    public static void addItemCreated(int itemId, String itemName, int amount) {
        if (!uniqueItemIds.contains(itemId)) {
            uniqueItemIds.add(itemId);
            itemCounter.labelValues(itemName, STATUS_CREATED).inc(amount);
            while (uniqueItemIds.size() > 1000) {
                uniqueItemIds.removeFirst();
            }
        }
    }
}
