package org.icetank.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import org.icetank.MetricsPlugin;
import org.icetank.module.MetricsModule;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
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
                        "port [port] - Set or views the metrics server port. 0 to assign a random port.",
                        "serviceDiscovery [on/off] - Enable or disable service discovery for the metrics server.",
                        "serviceDiscovery accountName [accountName] - Set the account name service discovery metrics label.",
                        "serviceDiscovery host [host] - Service discovery host.",
                        "serviceDiscovery port [port] - Service discovery port.",
                        "serviceDiscovery target [target] - Set the target host that Prometheus should scrape.")
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
                                    .title("Metrics module " + toggleStrCaps(MetricsPlugin.PLUGIN_CONFIG.enabled));
                        }))
                .then(literal("port").executes(c -> {
                            c.getSource().getEmbed()
                                    .title("Current metrics server port: " +
                                            MetricsPlugin.PLUGIN_CONFIG.port);
                        })
                        .then(argument("port", integer()).executes(c -> {
                            int oldPort = MetricsPlugin.PLUGIN_CONFIG.port;
                            int port = getInteger(c, "port");
                            if (port < 0 || port > 65535) {
                                c.getSource().getEmbed()
                                        .title("Invalid port: " + port);
                                return ERROR;
                            }
                            MetricsPlugin.PLUGIN_CONFIG.port = port;
                            if (port != oldPort) {
                                MODULE.get(MetricsModule.class).restartMetricsServer();
                                c.getSource().getEmbed()
                                        .title("Metrics server port changed to: " + port +
                                                ", restarting metrics server.");
                            } else {
                                c.getSource().getEmbed()
                                        .title("Metrics server port set to: " + port);
                            }
                            return OK;
                        })))
                .then(literal("serviceDiscovery")
                        .then(argument("enabled", toggle())
                                .executes(c -> {
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.enabled = getToggle(c, "enabled");
                                    c.getSource().getEmbed()
                                            .title("Service discovery is " +
                                                    toggleStrCaps(MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.enabled));
                                }))
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
                                        return ERROR;
                                    }
                                    MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.port = port;
                                    c.getSource().getEmbed()
                                            .title("Service discovery port set to: " + port);
                                    return OK;
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

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
                .primaryColor()
                .addField("Enabled", String.valueOf(MetricsPlugin.PLUGIN_CONFIG.enabled))
                .addField("Metrics Port", String.valueOf(MetricsPlugin.PLUGIN_CONFIG.port))
                .addField("Service Discovery Enabled", String.valueOf(MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.enabled))
                .addField("Service Discovery Account Name", MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.accountName)
                .addField("Service Discovery Host:Port", MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.host + ":" +
                        MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.port)
                .addField("Service Discovery Target Host", MetricsPlugin.PLUGIN_CONFIG.serviceDiscovery.targetHost);
    }
}
