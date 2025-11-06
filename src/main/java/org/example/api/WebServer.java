package org.example.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.zenith.Globals;
import io.javalin.Javalin;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import io.javalin.json.JavalinJackson;
import org.example.module.MetricsModule;

import static org.example.MetricsPlugin.PLUGIN_CONFIG;
import static org.example.MetricsPlugin.LOG;

public class WebServer {
    private Javalin server;

    public synchronized int getPort() {
        if (server != null) {
            return server.port();
        }
        return -1;
    }

    public synchronized void start() {
        if (server != null) {
            stop();
        }
        server = createServer();
        server.start(PLUGIN_CONFIG.metricsServer.host, PLUGIN_CONFIG.metricsServer.port);
        LOG.info("Web API started on port {}", server.port());
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
            LOG.info("Web API stopped");
        }
    }

    public synchronized boolean isRunning() {
        return server != null && server.jettyServer().started();
    }

    private Javalin createServer() {
        return Javalin.create(config -> {
            var threadPool = new ExecutorThreadPool();
            threadPool.setDaemon(true);
            threadPool.setName("ZenithProxy-WebAPI-%d");
            config.jetty.threadPool = threadPool;
            config.http.defaultContentType = "text/plain; charset=utf-8";
            var objectMapper = JavalinJackson.defaultMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            config.jsonMapper(new JavalinJackson(objectMapper, false));
        }).get("/metrics", ctx -> {
            if (!PLUGIN_CONFIG.publishMetrics) {
                ctx.status(HttpStatus.SERVICE_UNAVAILABLE_503);
                return;
            }
            var module = Globals.MODULE.get(MetricsModule.class);
            String metricsText = module.getMetricsText();
            ctx.result(metricsText);
            ctx.status(200);
        });
    }
}
