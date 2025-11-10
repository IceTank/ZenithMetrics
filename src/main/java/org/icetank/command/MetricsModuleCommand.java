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
                        "[on/off] - Toggle metrics publishing.",
                        "serviceDiscovery accountName <accountName> - Set the service account name used for metrics service discovery.",
                        "serviceDiscovery host <host> - Service discovery host.",
                        "serviceDiscovery port <port> - Service discovery port.",
                        "serviceDiscovery target <target> - Set the target host that Prometheus should scrape.")
                .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("metrics")
                .executes(c -> {
                    c.getSource().getEmbed()
                            .title("Metrics module is " + toggleStrCaps(MetricsPlugin.PLUGIN_CONFIG.enabled));
                })
                .then(argument("toggle", toggle())
                        .executes(c -> {
                            MetricsPlugin.PLUGIN_CONFIG.enabled = getToggle(c, "toggle");
                            MODULE.get(MetricsModule.class).syncEnabledFromConfig();
                            c.getSource().getEmbed()
                                    // if no title is set, no embed response will be sent
                                    // other properties like fields can be left unset without issues
                                    .title("Metrics module " + toggleStrCaps(MetricsPlugin.PLUGIN_CONFIG.enabled));
                        }))
                .then(literal("serviceDiscovery")
                        .executes(c -> {
                            c.getSource().getEmbed()
                                    .title("Service discovery configuration")
                                    .addField("Account Name", MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.accountName)
                                    .addField("Host", MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.host)
                                    .addField("Port", String.valueOf(MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.port))
                                    .addField("Target Host", MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.targetHost);
                        })
                        .then(literal("accountName")
                                .executes(c -> {
                                    c.getSource().getEmbed()
                                            .title("Current metrics account name: " +
                                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.accountName);
                                })
                                .then(argument("accountName", string()).executes(c -> {
                                    String accountName = getString(c, "accountName");
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.accountName = accountName;
                                    c.getSource().getEmbed()
                                            .title("Metrics account name set to: " + accountName);
                                })))
                        .then(literal("host")
                                .executes(c -> {
                                    c.getSource().getEmbed()
                                            .title("Current service discovery host: " +
                                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.host);
                                })
                                .then(argument("host", string()).executes(c -> {
                                    String host = getString(c, "host");
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.host = host;
                                    c.getSource().getEmbed()
                                            .title("Service discovery host set to: " + host);
                                })))
                        .then(literal("port")
                                .executes(c -> {
                                    c.getSource().getEmbed()
                                            .title("Current service discovery port: " +
                                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.port);
                                })
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
                                            .title("Service discovery port set to: " + port);
                                }))
                        )
                        .then(literal("target")
                                .executes(c -> {
                                    c.getSource().getEmbed()
                                            .title("Current metrics scrape target host: " +
                                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.targetHost);
                                })
                                .then(argument("target", string()).executes(c -> {
                                    String target = getString(c, "target");
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.targetHost = target;
                                    c.getSource().getEmbed()
                                            .title("Metrics scrape target host set to: " + target);
                                }))
                        )
                );
    }
}
