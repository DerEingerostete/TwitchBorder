package de.dereingerostete.border.util;

import de.dereingerostete.border.BorderPlugin;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class SchedulerUtils {
    private static final @NotNull BukkitScheduler SCHEDULER = Bukkit.getScheduler();
    private static final @NotNull Plugin PLUGIN = BorderPlugin.getInstance();

    public static void runAsync(@NotNull Runnable runnable) {
        SCHEDULER.runTaskAsynchronously(PLUGIN, runnable);
    }

    public static void runSync(@NotNull Runnable runnable) {
        SCHEDULER.runTask(PLUGIN, runnable);
    }

    public static void runAsyncLater(@NotNull Runnable runnable, long runAfter) {
        SCHEDULER.runTaskLaterAsynchronously(PLUGIN, runnable, runAfter);
    }

    public static void runSyncTask(@NotNull Runnable runnable, long delay, long period, @NotNull TimeUnit unit) {
        long delayTicks = unit.toSeconds(delay) * 20L;
        long periodTicks = unit.toSeconds(period) * 20L;
        SCHEDULER.runTaskTimer(PLUGIN, runnable, delayTicks, periodTicks);
    }

    @NotNull
    public static BukkitTask runAsyncTask(@NotNull Runnable runnable, long delay, long period, @NotNull TimeUnit unit) {
        long delayTicks = unit.toSeconds(delay) * 20L;
        long periodTicks = unit.toSeconds(period) * 20L;
        return SCHEDULER.runTaskTimerAsynchronously(PLUGIN, runnable, delayTicks, periodTicks);
    }

}
