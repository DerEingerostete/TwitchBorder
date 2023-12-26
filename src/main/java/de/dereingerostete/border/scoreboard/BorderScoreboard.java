package de.dereingerostete.border.scoreboard;

import com.google.common.collect.Maps;
import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.entity.BorderManager;
import de.dereingerostete.border.util.SchedulerUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BorderScoreboard {
    private final @Nullable AbstractScoreboard scoreboard;
    private final @NotNull Map<Integer, String> defaultLines;
    private final boolean enabled;

    public BorderScoreboard() {
        defaultLines = Maps.newHashMap();

        FileConfiguration config = BorderPlugin.getInstance().getConfig();
        enabled = config.getBoolean("scoreboard.enabled");

        if (enabled) {
            scoreboard = new AbstractScoreboard("undefined");
            loadScoreboard(config);
            SchedulerUtils.runSyncTask(this::updateAll, 1L, 5L, TimeUnit.SECONDS);
        } else scoreboard = null;
    }

    private void loadScoreboard(@NotNull FileConfiguration config) {
        if (scoreboard == null) throw new AssertionError(); // Should never happen

        String displayName = config.getString("scoreboard.name", "");
        scoreboard.setDisplayName(displayName);
        defaultLines.clear();

        ConfigurationSection section = config.getConfigurationSection("scoreboard.values");
        if (section == null) return;

        Set<String> keys = section.getKeys(false);
        keys.forEach(key -> {
            int line;
            try {
                line = Integer.parseInt(key);
            } catch (NumberFormatException exception) {
                return;
            }

            String text = section.getString(key, null);
            if (text == null) return;

            text = text.replaceAll("&", "§");
            defaultLines.put(line, text);
        });
    }

    public void updateAll() {
        if (!enabled || scoreboard == null) return;

        BorderManager manager = BorderPlugin.getBorderManager();
        String borderSize = String.format(Locale.US, "%,.2f", manager.getBorderSize());

        Map<Integer, String> lines = Maps.newHashMap();
        defaultLines.forEach((line, text) -> {
            String value = text.replaceAll("%size%", borderSize);
            lines.put(line, value);
        });
        scoreboard.setLines(lines);
    }

    public void addPlayer(@NotNull Player player) {
        if (enabled && scoreboard != null) scoreboard.addPlayer(player);
    }

}
