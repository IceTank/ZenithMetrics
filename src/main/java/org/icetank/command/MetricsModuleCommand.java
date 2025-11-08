package org.icetank.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import org.icetank.MetricsPlugin;
import org.icetank.module.MetricsModule;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static com.zenith.Globals.MODULE;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class MetricsModuleCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
                .name("metrics")
                .category(CommandCategory.MODULE)
                .description("Metrics module commands")
                .usageLines("Metrics module commands",
                        "[on/off] - Toggle metrics publishing",
                        "serviceDiscovery serviceId <serviceId> - Set the service ID for metrics service discovery",
                        "serviceDiscovery host <host> - Set the host for metrics service discovery",
                        "serviceDiscovery port <port> - Set the port for metrics service discovery")
                .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("metrics")
                .then(argument("toggle", toggle())
                        .executes(c -> {
                            MetricsPlugin.PLUGIN_CONFIG.enabled = getToggle(c, "toggle");
                            MODULE.get(MetricsModule.class).syncEnabledFromConfig();
                            c.getSource().getEmbed()
                                    // if no title is set, no embed response will be sent
                                    // other properties like fields can be left unset without issues
                                    .title("Metrics Module " + toggleStrCaps(MetricsPlugin.PLUGIN_CONFIG.enabled));
                        }))
                .then(literal("serviceDiscovery")
                        .then(literal("serviceId")
                                .then(argument("serviceId", string()).executes(c -> {
                                    String serviceId = getString(c, "serviceId");
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.serviceId = serviceId;
                                    c.getSource().getEmbed()
                                            .title("Metrics Service ID set to " + serviceId);
                                })))
                        .then(literal("host")
                                .then(argument("host", string()).executes(c -> {
                                    String host = getString(c, "host");
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.host = host;
                                    c.getSource().getEmbed()
                                            .title("Metrics Service Host set to " + host);
                                })))
                        .then(literal("port")
                                .then(argument("port", string()).executes(c -> {
                                    String portStr = getString(c, "port");
                                    int port;
                                    try {
                                        port = Integer.parseInt(portStr);
                                    } catch (NumberFormatException e) {
                                        c.getSource().getEmbed()
                                                .title("Invalid port: " + portStr);
                                        return;
                                    }
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.port = port;
                                    c.getSource().getEmbed()
                                            .title("Metrics Service Port set to " + port);
                                }))
                        )
                        .then(literal("target")
                                .then(argument("target", string()).executes(c -> {
                                    String target = getString(c, "target");
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.targetHost = target;
                                    c.getSource().getEmbed()
                                            .title("Metrics Service Host set to " + target);
                                }))
                        )
                );
    }
}
