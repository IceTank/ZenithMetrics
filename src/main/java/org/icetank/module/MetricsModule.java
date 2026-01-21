package org.icetank.module;

import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import com.zenith.network.codec.PacketHandlerCodec;
import com.zenith.network.codec.PacketHandlerStateCodec;
import com.zenith.util.ComponentSerializer;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import org.icetank.api.ServiceAnnouncer;
import org.icetank.metric.Metrics;
import org.icetank.metric.metrics.EntitiesInfo;
import org.icetank.metric.metrics.GameInfo;
import org.icetank.metric.metrics.ItemDrops;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.zenith.Globals.CACHE;
import static org.icetank.MetricsPlugin.LOG;
import static org.icetank.MetricsPlugin.PLUGIN_CONFIG;

public class MetricsModule extends Module {
    HTTPServer metricsServer = null;
    private ScheduledExecutorService scheduler = null;
    private final long SERVICE_HEARTBEAT_INTERVAL = 10;
    private final static Pattern regexFooterPattern = Pattern.compile("(\\d+(?:\\.\\d+)?) tps — (\\d+) players online — (\\d+) ping");

    @Override
    public boolean enabledSetting() {
        return PLUGIN_CONFIG.enabled;
    }

    @Override
    public void onEnable() {
        JvmMetrics.builder().register();
        Metrics.builder().register();

        startMetricsServer();
    }

    @Override
    public @Nullable PacketHandlerCodec registerClientPacketHandlerCodec() {
        return PacketHandlerCodec.clientBuilder()
                .setPriority(-5) // Does not really matter when we run as we only listen for entity creation events.
                .setId("metrics_packet_listener")
                .state(ProtocolState.GAME, PacketHandlerStateCodec.clientBuilder()
                        .inbound(ClientboundSetEntityDataPacket.class, new ClientboundEntityMetadataPacketHandler())
                        .inbound(ClientboundAddEntityPacket.class, new ClientboundAddEntityPacketHandler())
                        .inbound(ClientboundTabListPacket.class, new TabListPacketHandler())
                        .build())
                .build();
    }

    @Override
    public void onDisable() {
        shutdown();
    }

    private void startMetricsServer() {
        try {
            if (metricsServer != null) {
                LOG.warn("Metrics server is already running. (??????)");
                return;
            }
            int port = PLUGIN_CONFIG.port;
            metricsServer = HTTPServer.builder()
                    .port(port)
                    .buildAndStart();

            if (!PLUGIN_CONFIG.serviceDiscovery.enabled) {
                return;
            }

            // Start service discovery registration
            String accountName = PLUGIN_CONFIG.serviceDiscovery.accountName;
            String serviceName = "zenith-proxy-" + accountName;
            Map<String, String> labels = new HashMap<>(PLUGIN_CONFIG.serviceDiscovery.labels);
            labels.put("accountName", accountName); // For grafana queries
            labels.put("instance", PLUGIN_CONFIG.serviceDiscovery.targetHost); // Prevent cardinality explosion

            if (accountName.isEmpty()) {
                LOG.error("Service ID is empty, configure with zenith instance name");
                throw new RuntimeException("Service ID is empty");
            }
            if (metricsServer.getPort() <= 0) {
                LOG.error("Metrics server failed to start, cannot announce serviceName");
                throw new RuntimeException("Metrics server failed to start");
            }
            String target = PLUGIN_CONFIG.serviceDiscovery.targetHost + ":" + metricsServer.getPort();

            ServiceAnnouncer.ServiceInfo info = new ServiceAnnouncer.ServiceInfo(accountName, serviceName, target, labels, 60);
            CompletableFuture.runAsync(() -> {
                try {
                    ServiceAnnouncer.registerService(PLUGIN_CONFIG.serviceDiscovery.host,
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
                        ServiceAnnouncer.sendHeartbeat(PLUGIN_CONFIG.serviceDiscovery.host,
                                PLUGIN_CONFIG.serviceDiscovery.port, info);
                    } catch (Exception ex) {
                        LOG.error("Failed to send heartbeat for serviceName {}: {}", accountName, ex.getMessage());
                    }
                }, SERVICE_HEARTBEAT_INTERVAL, SERVICE_HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
            });
        } catch (Exception e) {
            disable();
            LOG.error("Failed to start MetricsModule, disabling module.", e);
        }
    }

    private void shutdown() {
        if (metricsServer != null) {
            metricsServer.stop();
            metricsServer = null;
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public void restartMetricsServer() {
        shutdown();
        startMetricsServer();
    }

    private static class ClientboundEntityMetadataPacketHandler implements ClientEventLoopPacketHandler<ClientboundSetEntityDataPacket, ClientSession> {
        /**
         * When minecraft spawns an item it usually spawns an item entity first and then assigns the itemStack to it
         * with a EntityData packet. I think. Don't quote me on that.
         * We check if this metadata packet belongs to an item entity, and if so we extract the itemStack from it and
         * it as a created item.
         *
         * @param packet  The packet
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

    private static class ClientboundAddEntityPacketHandler implements ClientEventLoopPacketHandler<ClientboundAddEntityPacket, ClientSession> {
        @Override
        public boolean applyAsync(ClientboundAddEntityPacket packet, ClientSession session) {
            EntitiesInfo.incrementEntityCounter(packet.getType(), packet.getEntityId());
            EntitiesInfo.recordHighestEntityId(packet.getEntityId());
            return true;
        }
    }

    private static class TabListPacketHandler implements ClientEventLoopPacketHandler<ClientboundTabListPacket, ClientSession> {
        @Override
        public boolean applyAsync(ClientboundTabListPacket packet, ClientSession session) {
            String footer = ComponentSerializer.serializePlain(packet.getFooter());
            footer = footer.replaceAll("§.", ""); // Remove color codes
            try {
                // Example footer: 19.51 tps — 679 players online — 127 ping
                var match = regexFooterPattern.matcher(footer);
                if (match.find() && match.groupCount() == 3) {
                    double tps = Double.parseDouble(match.group(1));
                    int playerCount = Integer.parseInt(match.group(2));
                    int ping = Integer.parseInt(match.group(3));
                    GameInfo.reportedTPS.set(tps);
                    GameInfo.reportedPlayerCount.set(playerCount);
                    GameInfo.reportedPing.set(ping);
                }
            } catch (Exception e) {
                LOG.warn("Failed to parse tab list footer for metrics: {}", footer);
            }
            return true;
        }
    }
}
