package de.dereingerostete.border.listener;

import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.chat.Chat;
import de.dereingerostete.border.chat.Logging;
import de.dereingerostete.border.database.BorderDatabase;
import de.dereingerostete.border.entity.BorderManager;
import de.dereingerostete.border.util.SchedulerUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.math.RoundingMode;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.UUID;

public class ConnectListener implements Listener {
    private final @NotNull DecimalFormat decimalFormat;
    private final @NotNull String noDifferenceMessage;
    private final @NotNull String increaseMessage;
    private final @NotNull String decreaseMessage;

    public ConnectListener() {
        this.decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setRoundingMode(RoundingMode.UP);

        FileConfiguration config = BorderPlugin.getInstance().getConfig();
        noDifferenceMessage = config.getString("messages.join.noDifference", "noDifference");
        increaseMessage = config.getString("messages.join.increase", "increaseMessage");
        decreaseMessage = config.getString("messages.join.decrease", "decreaseMessage");
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        BorderPlugin.getScoreboard().addPlayer(player);
        SchedulerUtils.runAsync(() -> {
            Double lastSize;
            try {
                BorderDatabase database = BorderPlugin.getDatabase();
                lastSize = database.getPlayerBorderSize(uuid);

                if (lastSize == null) return;
            } catch (SQLException exception) {
                Logging.warning("Failed to load border size from database", exception);
                return;
            }

            BorderManager manager = BorderPlugin.getBorderManager();
            double difference = manager.getBorderSize() - lastSize;
            String formattedDifference = decimalFormat.format(Math.abs(difference));

            if (difference == 0) {
                Chat.toPlayer(player, noDifferenceMessage);
            } else if (difference > 0) {
                String message = increaseMessage.replace("%amount%", formattedDifference);
                Chat.toPlayer(player, message);
            } else {
                String message = decreaseMessage.replace("%amount%", formattedDifference);
                Chat.toPlayer(player, message);
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        SchedulerUtils.runAsync(() -> {
            try {
                BorderManager manager = BorderPlugin.getBorderManager();
                double borderSize = manager.getBorderSize();

                BorderDatabase database = BorderPlugin.getDatabase();
                database.setPlayer(uuid, borderSize);
            } catch (SQLException exception) {
                Logging.warning("Failed to save border size of player in database", exception);
            }
        });
    }

}