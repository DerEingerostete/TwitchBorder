package de.dereingerostete.border.chat;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Chat {
    private static final @NotNull String PREFIX = "§8[§5Twitch§8] §7";

    @NotNull
    public static String getPrefix() {
        return PREFIX;
    }

    public static void toPlayer(@NotNull CommandSender sender, @NotNull String message) {
        sender.sendMessage(PREFIX + message);
    }

    public static void toConsole(@NotNull String message) {
        toConsole(message, true);
    }

    public static void toConsole(@NotNull String message, boolean usePrefix) {
        CommandSender sender = Bukkit.getConsoleSender();
        if (!usePrefix) sender.sendMessage(message);
        else toPlayer(sender, message);
    }

    public static void broadcast(@NotNull String message) {
        Bukkit.broadcastMessage(PREFIX + message);
    }

}
