package de.dereingerostete.border.entity.twitch;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

@Data
@AllArgsConstructor
public class ApplicationData {
    private @NotNull String clientId;
    private @NotNull String clientSecret;

    public ApplicationData(@Nullable ConfigurationSection section) {
        if (section == null) throw new IllegalStateException("Twitch section is not defined in config");
        this.clientId = Objects.requireNonNull(section.getString("clientId"));
        this.clientSecret = Objects.requireNonNull(section.getString("clientSecret"));
    }

}
