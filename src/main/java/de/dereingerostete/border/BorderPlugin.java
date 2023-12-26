package de.dereingerostete.border;

import de.dereingerostete.border.chat.Logging;
import de.dereingerostete.border.command.BorderSizeCommand;
import de.dereingerostete.border.command.GrowBorderCommand;
import de.dereingerostete.border.command.TwitchCommand;
import de.dereingerostete.border.command.TwitchTestCommand;
import de.dereingerostete.border.command.util.SimpleCommand;
import de.dereingerostete.border.database.BorderDatabase;
import de.dereingerostete.border.entity.BorderManager;
import de.dereingerostete.border.entity.twitch.TwitchCredentials;
import de.dereingerostete.border.entity.twitch.TwitchIntegration;
import de.dereingerostete.border.listener.ConnectListener;
import de.dereingerostete.border.scoreboard.BorderScoreboard;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class BorderPlugin extends JavaPlugin {
    private static @Getter Plugin instance;
    private static @Getter BorderDatabase database;
    private static @Getter ConfigurationSection messagesSection;
    private static @Getter BorderManager borderManager;
    private static @Getter BorderScoreboard scoreboard;

    //Twitch
    private static @Getter TwitchCredentials credentials;
    private static @Getter TwitchIntegration integration;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        messagesSection = getConfig().getConfigurationSection("messages");
        borderManager = new BorderManager();
        scoreboard = new BorderScoreboard();

        try {
            File file = new File(getDataFolder(), "sqlite.db");
            database = new BorderDatabase(file);
        } catch (SQLException exception) {
            Logging.warning("Failed to connect to database", exception);
            return;
        }

        try {
            credentials = new TwitchCredentials();
            credentials.startRefresh();
            credentials.refresh();
        } catch (IOException exception) {
            Logging.warning("Failed to load Twitch credentials", exception);
        }

        integration = new TwitchIntegration();
        PluginManager manager = Bukkit.getPluginManager();
        manager.registerEvents(new ConnectListener(), this);

        registerCommand(new TwitchCommand());
        registerCommand(new TwitchTestCommand());
        registerCommand(new BorderSizeCommand());
        registerCommand(new GrowBorderCommand());
        Logging.info("Plugin enabled");
    }

    private void registerCommand(@NotNull SimpleCommand command) {
        command.register("border");
    }

    @Override
    public void onDisable() {
        try {
            database.disconnect();
        } catch (SQLException exception) {
            Logging.warning("Failed to disconnect from database", exception);
        }

        if (integration != null) integration.close();
        credentials.stopRefresh();
        borderManager.stop();

        Logging.info("Plugin disabled");
    }

    @NotNull
    public static String getMessage(@NotNull String key) {
        return messagesSection.getString(key, key);
    }

}