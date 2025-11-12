package org.icetank.api;

import com.google.gson.Gson;
import org.icetank.MetricsPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class ServiceAnnouncer {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void registerService(String host, int port, ServiceInfo serviceInfo) throws IOException, InterruptedException {
        URI url = buildUri(host, port, "/register");

        MetricsPlugin.LOG.info("Registering metrics serviceName at {}", url);
        MetricsPlugin.LOG.info("Metric Service info:\n{}", serviceInfo.toString());

        HttpRequest req = HttpRequest.newBuilder(url)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(serviceInfo.toJson()))
                .build();

        int statusCode = client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
        if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_NO_CONTENT) {
            throw new IOException("Failed to register serviceName: received status code " + statusCode);
        }
    }

    public static void sendHeartbeat(String host, int port, ServiceInfo info) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            URI url = URI.create("http://" + host + ":" + port + "/heartbeat");

            String heartbeatJson = String.format("{\"id\":\"%s\"}", info.id());

            HttpRequest req = HttpRequest.newBuilder(url)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(heartbeatJson))
                    .build();

            HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                MetricsPlugin.LOG.warn("Service not found for heartbeat, re-registering serviceName {}", info.id());
                registerService(host, port, info);
                return;
            }
            if (statusCode != HttpURLConnection.HTTP_OK && statusCode != HttpURLConnection.HTTP_NO_CONTENT) {
                MetricsPlugin.LOG.warn("Failed to send heartbeat for serviceName {}: received status code {}", info.id(), statusCode);
            }
        } catch (IOException | InterruptedException e) {
            MetricsPlugin.LOG.warn("Exception while sending heartbeat for serviceName {}", info.id(), e);
        }
    }

    private static URI buildUri(String host, int port, String path) {
        if (path == null) path = "/";
        if (!path.startsWith("/")) path = "/" + path;

        String scheme = "http";
        String cleanedHost = host;
        if (host.startsWith("http://")) {
            scheme = "http";
            cleanedHost = host.substring(7);
        } else if (host.startsWith("https://")) {
            scheme = "https";
            cleanedHost = host.substring(8);
        }
        // remove trailing slashes from host
        cleanedHost = cleanedHost.replaceAll("/+$", "");
        return URI.create(scheme + "://" + cleanedHost + ":" + port + path);
    }

    public record ServiceInfo(
            String id,
            String service,
            String target,
            Map<String, String> labels,
            int ttl_seconds
    ) {
        String toJson() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }

        @Override
        public @NotNull String toString() {
            return toJson();
        }
    }
}
