package de.dereingerostete.border.command;

import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.chat.Chat;
import de.dereingerostete.border.command.util.SimpleCommand;
import de.dereingerostete.border.entity.BorderManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GrowBorderCommand extends SimpleCommand {

    public GrowBorderCommand() {
        super("twitchtest", true);
        setDescription("Used for testing of Twitch Events");
        setPermission("twitch.growborder");
        setUsage("/" + getName() + " <amount>");
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args, int arguments) {
        if (arguments == 0) {
            Chat.toPlayer(sender, "§cWrong usage!§7 Use " + getUsage());
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException exception) {
            Chat.toPlayer(sender, "§cWrong usage!§7 Use " + getUsage());
            return;
        }

        if (amount <= 0) {
            Chat.toPlayer(sender, "§cAmount must be greater than 0!");
            return;
        }

        BorderManager manager = BorderPlugin.getBorderManager();
        manager.increase(amount);
        Chat.toPlayer(sender, "§7Successfully increased border by §a" + amount + " blocks!");
    }

}