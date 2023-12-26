package de.dereingerostete.border.chat;

import de.dereingerostete.border.BorderPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Logging {
    private static final @NotNull Logger logger = BorderPlugin.getInstance().getLogger();

    public static void severe(@NotNull String message, @NotNull Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    public static void severe(@NotNull String message) {
        logger.severe(message);
    }

    public static void warning(@NotNull String message, @NotNull Throwable throwable) {
        logger.log(Level.WARNING, message, throwable);
    }

    public static void warning(@NotNull String message) {
        logger.warning(message);
    }

    public static void info(@NotNull String message) {
        logger.info(message);
    }

}
