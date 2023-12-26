package de.dereingerostete.border.entity.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.chat.Logging;
import lombok.Data;
import lombok.Setter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Data
public class OAuthData {
    private @Setter @Nullable ConfigurationSection section;
    private @NotNull String accessToken;
    private @NotNull String refreshToken;
    private @NotNull Instant expireDate;
    private @NotNull String displayName;
    private @NotNull String channelId;

    public OAuthData(@NotNull ConfigurationSection authSection) {
        this.section = authSection;
        this.accessToken = getNotNullString(section, "accessToken");
        this.refreshToken = getNotNullString(section, "refreshToken");
        this.displayName = getNotNullString(section, "displayName");
        this.channelId = getNotNullString(section, "channelId");

        String expireDate = getNotNullString(section, "expireDate");
        this.expireDate = Instant.parse(expireDate);
    }

    public OAuthData(@NotNull JSONObject object) throws IOException {
        update(object);
    }

    @Nullable
    public OAuth2Credential toCredential() {
        return new OAuth2Credential("twitch", accessToken);
    }

    public void update(@NotNull JSONObject responseObject) throws IOException {
        this.accessToken = responseObject.getString("access_token");
        this.refreshToken = responseObject.getString("refresh_token");

        long expiresIn = responseObject.getLong("expires_in");
        this.expireDate = Instant.now().plusSeconds(expiresIn);

        loadUserInfo();
        updateConfig();
    }

    public void refresh(@NotNull ApplicationData data) throws IOException, InterruptedException {
        String body = "grant_type=refresh_token&refresh_token=" + refreshToken +
                "&client_id=" + data.getClientId() + "&client_secret=" + data.getClientSecret();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://id.twitch.tv/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .method("POST", HttpRequest.BodyPublishers.ofString(body))
                .build();

        JSONObject object;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            object = new JSONObject(response.body());
        } catch (JSONException exception) {
            throw new IOException(exception);
        }

        try {
            update(object);
        } catch (JSONException exception) {
            Logging.warning("Failed to handle OAuth update. Response: " + object);
            throw new IOException(exception);
        }
    }

    public void updateConfig() {
        if (section == null) return;
        section.set("accessToken", accessToken);
        section.set("refreshToken", refreshToken);
        section.set("expireDate", expireDate.toString());
        section.set("displayName", displayName);
        section.set("channelId", channelId);
        BorderPlugin.getInstance().saveConfig();
    }

    private void loadUserInfo() throws IOException {
        TwitchCredentials credentials = BorderPlugin.getCredentials();
        String clientId = credentials.getApplicationData().getClientId();

        Request request = new Request.Builder()
                .url("https://api.twitch.tv/helix/users")
                .get()
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Client-Id", clientId)
                .build();

        OkHttpClient client = new OkHttpClient();
        Response response = client.newCall(request).execute();
        ResponseBody body = response.body();
        if (body == null) throw new IOException("Failed to load display name: Response body is null");

        String bodyString = body.string();
        response.close();
        try {
            JSONObject object = new JSONObject(bodyString);
            JSONArray dataArray = object.getJSONArray("data");

            JSONObject userObject = dataArray.getJSONObject(0);
            this.channelId = userObject.getString("id");
            this.displayName = userObject.getString("display_name");
        } catch (JSONException exception) {
            throw new IOException(exception);
        }
    }

    @NotNull
    private String getNotNullString(@NotNull ConfigurationSection section, @NotNull String key) {
        String value = section.getString(key);
        if (value == null) throw new IllegalStateException("Value of '" + key + "' is null");
        return value;
    }

}
