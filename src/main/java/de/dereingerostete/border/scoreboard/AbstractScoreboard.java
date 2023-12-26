package de.dereingerostete.border.scoreboard;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class AbstractScoreboard {
    protected final @NotNull Scoreboard scoreboard;
    protected @NotNull String displayName;
    protected @Nullable Objective objective;

    public AbstractScoreboard(@NotNull String displayName) {
        this.displayName = displayName;
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Validate.notNull(manager, "ScoreboardManger is not loaded");

        scoreboard = manager.getNewScoreboard();
        registerObjective();
        List.copyOf(Bukkit.getOnlinePlayers()).forEach(player -> player.setScoreboard(scoreboard));
    }

    public void setDisplayName(@NotNull String displayName) {
        if (objective == null) return;
        objective.setDisplayName(displayName);
        this.displayName = displayName;
    }

    public void setLines(@NotNull Map<Integer, String> lines) {
        registerObjective();
        if (objective == null) throw new AssertionError(); //Objective should not be null here

        lines.forEach((line, text) -> {
            Score score = objective.getScore(text);
            score.setScore(line);
        });
    }

    protected void registerObjective() {
        if (objective != null) {
            objective.setDisplaySlot(null);
            objective.unregister();
        }

        String randomId = getRandomId();
        objective = scoreboard.registerNewObjective(randomId, Criteria.DUMMY, displayName, RenderType.INTEGER);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public void addPlayer(@NotNull Player player) {
        player.setScoreboard(scoreboard);
    }

    @NotNull
    protected String getRandomId() {
        return RandomStringUtils.random(12);
    }

}
