package de.dereingerostete.border.entity.twitch;

import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.util.SchedulerUtils;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Random;
import java.util.function.Function;

@Data
public class BorderGrowData {
    private final @NotNull Function<Integer, Double> growFunction;
    private final double fireworkAfter;
    private final boolean firework;

    public BorderGrowData(@NotNull Type type) {
        String configKey = type.getConfigKey();
        FileConfiguration config = BorderPlugin.getInstance().getConfig();
        this.fireworkAfter = config.getDouble(configKey + ".fireworkAfter", -1);
        this.firework = config.getBoolean(configKey + ".firework", false);

        double multiplier = config.getDouble(configKey + ".multiplier");
        this.growFunction = i -> i * multiplier;
    }

    public boolean showFirework(int amount) {
        return firework && amount >= fireworkAfter;
    }

    public void spawnFirework() {
        Color purpleColor = Color.fromRGB(145, 70, 255);
        Color purpleDarkerColor = Color.fromRGB(100, 65, 165);
        Color darkPurpleColor = Color.fromRGB(60, 45, 90);

        Random random = new Random();
        SchedulerUtils.runSync(() -> Bukkit.getWorlds().forEach(world -> {
            Location spawnLocation = world.getSpawnLocation();
            for (int i = 0; i < random.nextInt(8, 16); i++) {
                double x = random.nextDouble(0.5D, 8.5D);
                double z = random.nextDouble(0.5D, 8.5D);
                Location location = spawnLocation.clone().add(x, 0, z);
                world.spawn(location, Firework.class, firework -> {
                    firework.setShotAtAngle(false);

                    FireworkEffect.Type type;
                    if (random.nextInt(3) == 0) type = FireworkEffect.Type.BALL_LARGE;
                    else type = FireworkEffect.Type.BALL;

                    FireworkEffect effect = FireworkEffect.builder()
                            .withColor(purpleColor, purpleDarkerColor)
                            .withFade(darkPurpleColor)
                            .withFlicker()
                            .withTrail()
                            .with(type)
                            .build();

                    FireworkMeta meta = firework.getFireworkMeta();
                    meta.addEffect(effect);

                    int power = random.nextInt(1, 4);
                    meta.setPower(power);
                    firework.setFireworkMeta(meta);
                });
            }
        }));
    }

    @Getter
    @RequiredArgsConstructor
    public enum Type {
        GIFTED_SUBSCRIPTION("border.giftSub"),
        SUBSCRIPTION("border.subscription"),
        FOLLOW("border.follow"),
        BITS("border.bit");

        @NotNull
        private final String configKey;

    }

}
