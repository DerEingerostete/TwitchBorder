package de.dereingerostete.border.entity;

import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.util.SchedulerUtils;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
public class BorderManager {
    private final @NotNull BukkitTask task;
    private final int increaseTime;
    private double borderSize;

    public BorderManager() {
        loadBorderSize();

        Plugin plugin = BorderPlugin.getInstance();
        FileConfiguration configuration = plugin.getConfig();
        this.increaseTime = configuration.getInt("border.increaseTime");

        BukkitScheduler scheduler = Bukkit.getScheduler();
        task = scheduler.runTaskTimer(plugin, this::loadBorderSize, 1L, 1200L); //Every minute
    }

    public void stop() {
        this.task.cancel();
    }

    public void setSize(double size) {
        this.borderSize = size;
        SchedulerUtils.runSync(() -> {
            List<World> worlds = Bukkit.getWorlds();
            worlds.forEach(world -> {
                WorldBorder border = world.getWorldBorder();
                border.setSize(size, increaseTime);
            });
        });
    }

    public void increase(double value) {
        setSize(borderSize + value);
    }

    protected void loadBorderSize() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) {
            setBorderSize(0);
            return;
        }

        WorldBorder border = worlds.get(0).getWorldBorder();
        double currentSize = border.getSize();
        if (currentSize > this.borderSize) setSize(currentSize);
    }

}
