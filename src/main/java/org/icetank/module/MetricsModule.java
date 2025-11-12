package org.icetank.module;

import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import com.zenith.network.codec.PacketHandlerCodec;
import com.zenith.network.codec.PacketHandlerStateCodec;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.icetank.api.ServiceAnnouncer;
import org.icetank.metric.Metrics;
import org.icetank.metric.metrics.ItemDrops;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.zenith.Globals.CACHE;
import static org.icetank.MetricsPlugin.LOG;
import static org.icetank.MetricsPlugin.PLUGIN_CONFIG;

public class MetricsModule extends Module {
    HTTPServer server = null;
    ServiceAnnouncer announcer = new ServiceAnnouncer();
    private ScheduledExecutorService scheduler = null;
    private final long METRICS_INTERVAL_SECONDS = 10;

    @Override
    public boolean enabledSetting() {
        return PLUGIN_CONFIG.enabled;
    }

    @Override
    public void onEnable() {
        JvmMetrics.builder().register();
        Metrics.builder().register();

        try {
            if (server != null) {
                LOG.warn("Metrics server is already running.");
                return;
            }
            server = HTTPServer.builder()
                    .port(0)
                    .buildAndStart();

            String accountName = PLUGIN_CONFIG.serviceDiscovery.accountName;
            String serviceName = "zenith-proxy-" + accountName;
            Map<String, String> labels = new HashMap<>(PLUGIN_CONFIG.serviceDiscovery.labels);
            labels.put("accountName", accountName); // For grafana queries
            labels.put("instance", PLUGIN_CONFIG.serviceDiscovery.targetHost); // Prevent cardinality explosion

            if (accountName.isEmpty()) {
                LOG.error("Service ID is empty, configure with zenith instance name");
                throw new RuntimeException("Service ID is empty");
            }
            if (server.getPort() <= 0) {
                LOG.error("Metrics server failed to start, cannot announce serviceName");
                throw new RuntimeException("Metrics server failed to start");
            }
            String target = PLUGIN_CONFIG.serviceDiscovery.targetHost + ":" + server.getPort();

            ServiceAnnouncer.ServiceInfo info = new ServiceAnnouncer.ServiceInfo(accountName, serviceName, target, labels, 60);
            CompletableFuture.runAsync(() -> {
                try {
                    announcer.registerService(PLUGIN_CONFIG.serviceDiscovery.host,
                            PLUGIN_CONFIG.serviceDiscovery.port, info);
                } catch (Exception ex) {
                    LOG.error("Failed to register serviceName. Disabling.", ex);
                    disable();
                    return;
                }
                if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
                    scheduler = Executors.newSingleThreadScheduledExecutor();
                }
                scheduler.scheduleAtFixedRate(() -> {
                    try {
                        announcer.sendHeartbeat(PLUGIN_CONFIG.serviceDiscovery.host,
                                PLUGIN_CONFIG.serviceDiscovery.port, info);
                    } catch (Exception ex) {
                        LOG.error("Failed to send heartbeat for serviceName {}: {}", accountName, ex.getMessage());
                    }
                }, METRICS_INTERVAL_SECONDS, METRICS_INTERVAL_SECONDS, TimeUnit.SECONDS);
            });
        } catch (Exception e) {
            disable();
            LOG.error("Failed to start MetricsModule, disabling module.", e);
        }
    }

    @Override
    public @Nullable PacketHandlerCodec registerClientPacketHandlerCodec() {
        return PacketHandlerCodec.clientBuilder()
                .setPriority(5) // Before zenith Modules
                .setId("matrics_packet_listener")
                .state(ProtocolState.GAME, PacketHandlerStateCodec.clientBuilder()
                        //.inbound(ClientboundAddEntityPacket.class, new ClientboundAddEntityPacketHandler())
                        //.inbound(ClientboundRemoveEntitiesPacket.class, new ClientboundRemoveEntityPacket())
                        .inbound(ClientboundSetEntityDataPacket.class, new ClientboundEntityMetadataPacketHandler())
                        .build())
                .build();
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    private void shutdown() {
        if (server != null) {
            server.stop();
            server = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private static class ClientboundAddEntityPacketHandler implements ClientEventLoopPacketHandler<ClientboundAddEntityPacket, ClientSession> {
        @Override
        public boolean applyAsync(ClientboundAddEntityPacket packet, ClientSession session) {
            var entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (packet.getType() == EntityType.ITEM && entity instanceof EntityStandard) {
                var itemStack = entity.getMetadataValue(8, MetadataTypes.ITEM, ItemStack.class);
                if (itemStack != null) {
                    var itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
                    if (itemData != null) {
                        ItemDrops.itemCounter.labelValues(itemData.name(), ItemDrops.STATUS_CREATED).inc(itemStack.getAmount());
                    }
                }
            }
            return true;
        }
    }

    private static class ClientboundEntityMetadataPacketHandler implements ClientEventLoopPacketHandler<ClientboundSetEntityDataPacket, ClientSession> {
        /**
         * When minecraft spawns an item it usually spawns an item entity first and then assigns the itemStack to it with
         * a EntityData packet. I think.
         * We have to construct the item stack ourselves because we run before zenith's entity update handler so we can
         * observe when entities are removed from the cache.
         * TODO: What happens when item stacks merge?
         * @param packet The packet
         * @param session The session
         * @return true
         */
        @Override
        public boolean applyAsync(ClientboundSetEntityDataPacket packet, ClientSession session) {
            var entity = CACHE.getEntityCache().get(packet.getEntityId());
            if (entity != null && entity.getEntityType() == EntityType.ITEM) {
                for (var meta : packet.getMetadata()) {
                    if (meta.getId() == 8) {
                        var metadataValue = meta.getValue();
                        if (metadataValue instanceof ItemStack valueCast) {
                            ItemData itemData = ItemRegistry.REGISTRY.get(valueCast.getId());
                            if (itemData != null) {
                                ItemDrops.addItemCreated(packet.getEntityId(), itemData.name(), valueCast.getAmount());
                            }
                        }
                    }
                }
            }
            return true;
        }
    }

    private static class ClientboundRemoveEntityPacket implements ClientEventLoopPacketHandler<ClientboundRemoveEntitiesPacket, ClientSession> {
        @Override
        public boolean applyAsync(ClientboundRemoveEntitiesPacket packet, ClientSession session) {
            for (int entityId : packet.getEntityIds()) {
                var entity = CACHE.getEntityCache().get(entityId);
                if (entity instanceof EntityStandard entityStandard && entity.getEntityType() == EntityType.ITEM) {
                    var itemStack = entityStandard.getMetadataValue(8, MetadataTypes.ITEM, ItemStack.class);
                    if (itemStack != null) {
                        var itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
                        if (itemData != null) {
                            ItemDrops.itemCounter.labelValues(itemData.name(), ItemDrops.STATUS_DELETED).inc(itemStack.getAmount());
                        }
                    }
                }
            }
            return true;
        }
    }
}
