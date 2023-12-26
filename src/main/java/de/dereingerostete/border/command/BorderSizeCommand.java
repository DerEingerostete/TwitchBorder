package de.dereingerostete.border.command;

import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.command.util.SimpleCommand;
import de.dereingerostete.border.entity.BorderManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class BorderSizeCommand extends SimpleCommand {
    private final @NotNull String message;

    public BorderSizeCommand() {
        super("bordersize", true);
        addAliases("size");
        setDescription("Returns the current size of the border");
        message = BorderPlugin.getMessage("size");
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args, int arguments) {
        BorderManager manager = BorderPlugin.getBorderManager();
        String size = String.valueOf(manager.getBorderSize());
        sender.sendMessage(message.replace("%amount%", size));
    }

}