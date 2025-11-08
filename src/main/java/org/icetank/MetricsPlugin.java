package org.icetank;

import com.zenith.plugin.api.Plugin;
import com.zenith.plugin.api.PluginAPI;
import com.zenith.plugin.api.ZenithProxyPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.example.BuildConstants;
import org.icetank.command.MetricsModuleCommand;
import org.icetank.module.MetricsModule;

@Plugin(
    id = "metrics-plugin",
    version = BuildConstants.VERSION,
    description = "ZenithProxy Prometheus Metrics publishing plugin",
    url = "https://github.com/rfresh2/ZenithProxyExamplePlugin",
    authors = {"IceTank"},
    mcVersions = {"1.21.4"} // to indicate any MC version: @Plugin(mcVersions = "*")
                            // if you touch packet classes, you almost certainly need to pin to a single mc version
)
public class MetricsPlugin implements ZenithProxyPlugin {
    // public static for simple access from modules and commands
    // or alternatively, you could pass these around in constructors
    public static MetricsConfig PLUGIN_CONFIG;
    public static ComponentLogger LOG;

    @Override
    public void onLoad(PluginAPI pluginAPI) {
        LOG = pluginAPI.getLogger();
        LOG.info("Metrics Plugin loading...");
        // initialize any configurations before modules or commands might need to read them
        PLUGIN_CONFIG = pluginAPI.registerConfig("metrics-plugin", MetricsConfig.class);
        pluginAPI.registerModule(new MetricsModule());
        pluginAPI.registerCommand(new MetricsModuleCommand());
        LOG.info("Metrics Plugin loaded.");
    }
}
