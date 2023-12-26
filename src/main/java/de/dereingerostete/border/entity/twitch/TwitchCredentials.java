package de.dereingerostete.border.entity.twitch;

import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.chat.Logging;
import de.dereingerostete.border.util.SchedulerUtils;
import lombok.Data;
import org.apache.commons.lang.RandomStringUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Data
public class TwitchCredentials {
    private final @NotNull ApplicationData applicationData;
    private final @NotNull List<OAuthData> oAuthLogins;
    private @NotNull ConfigurationSection authSection;
    private @Nullable BukkitTask refreshTask;
    private @Nullable String state;

    public TwitchCredentials() throws IOException {
        FileConfiguration configuration = BorderPlugin.getInstance().getConfig();
        ConfigurationSection twitchSection = configuration.getConfigurationSection("twitch");
        applicationData = new ApplicationData(twitchSection);

        oAuthLogins = new ArrayList<>();
        ConfigurationSection authSection = twitchSection.getConfigurationSection("authentication");
        if (authSection != null) {
            this.authSection = authSection;
            Set<String> keys = authSection.getKeys(false);
            keys.forEach(key -> {
                ConfigurationSection section = authSection.getConfigurationSection(key);
                if (section == null) throw new IllegalStateException("OAuth section is null");

                OAuthData data = new OAuthData(section);
                oAuthLogins.add(data);
            });
        } else {
            this.authSection = twitchSection.createSection("authentication");
        }
    }

    @NotNull
    public String authenticate(@NotNull String code) throws IOException {
        String body = "client_id=" + applicationData.getClientId() + "&client_secret=" + applicationData.getClientSecret() +
                "&code=" + code + "&grant_type=authorization_code&redirect_uri=http%3A%2F%2Flocalhost";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://id.twitch.tv/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject responseObject = new JSONObject(response.body());

            OAuthData authData = new OAuthData(responseObject);
            String channelId = authData.getChannelId();

            ConfigurationSection section = authSection.createSection(channelId);
            authData.setSection(section);
            authData.updateConfig();

            TwitchIntegration integration = BorderPlugin.getIntegration();
            integration.addChannel(authData);
            return authData.getDisplayName();
        } catch (InterruptedException exception) {
            throw new IOException(exception);
        }
    }

    @Nullable
    public String createAuthURL() {
        this.state = RandomStringUtils.randomAlphabetic(32);
        return "https://id.twitch.tv/oauth2/authorize?response_type=code&client_id=" + applicationData.getClientId() +
                "&redirect_uri=http%3A%2F%2Flocalhost&scope=moderator%3Aread%3Afollowers%20bits%3Aread%20channel" +
                "%3Aread%3Asubscriptions%20channel_subscriptions&state=" + state;
    }

    public void startRefresh() {
        stopRefresh();
        refreshTask = SchedulerUtils.runAsyncTask(this::refresh, 60, 60, TimeUnit.MINUTES);
    }

    public void refresh() {
        oAuthLogins.forEach(oAuthData -> {
            try {
                Instant expireDate = oAuthData.getExpireDate().minus(5, ChronoUnit.MINUTES);;
                Instant now = Instant.now();
                if (now.isAfter(expireDate)) {
                    Logging.info("Refreshing OAuth data");
                    oAuthData.refresh(applicationData);
                }
            } catch (IOException | InterruptedException exception) {
                Logging.warning("Failed to refresh OAuth token", exception);
            }
        });
    }

    public void stopRefresh() {
        if (refreshTask != null) refreshTask.cancel();
    }

}
