package de.dereingerostete.border.command.util;

import de.dereingerostete.border.util.SchedulerUtils;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public abstract class SimpleCommand extends Command {
    protected final boolean async;

    public SimpleCommand(@NotNull String name, boolean async) {
        super(name);
        this.async = async;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender,
                           @NotNull String commandLabel, @NotNull String[] args) {
        Runnable runnable = () -> {
            if (!canExecute(sender)) return;

            int arguments = args.length;
            if (sender instanceof Player) execute((Player) sender, args, arguments);
            else execute(sender, args, arguments);
        };

        if (async) SchedulerUtils.runAsync(runnable);
        else runnable.run();
        return true;
    }

    /**
     * Adds a list of new aliases to the already existing ones
     * @param aliases The list of new aliases to add
     */
    protected void addAliases(String... aliases) {
        List<String> aliasList = getAliases();
        Collections.addAll(aliasList, aliases);
        setAliases(aliasList);
    }

    /**
     * Is called when a CommandSender calls the command
     * @param sender The sender executing the command
     * @param args The specified arguments
     * @param arguments The amount of specified arguments
     */
    public abstract void execute(@NotNull CommandSender sender, @NotNull String[] args, int arguments);

    /**
     * Is called when a Player calls the command
     * @param player The sender executing the command
     * @param args The specified arguments
     * @param arguments The amount of specified arguments
     */
    public void execute(@NotNull Player player, @NotNull String[] args, int arguments) {
        execute((CommandSender) player, args, arguments);
    }

    /**
     * Is called to check if the sender can execute the command
     * @param sender The sender executing the command
     * @return True if the sender can execute this command; False if not
     */
    public boolean canExecute(@NotNull CommandSender sender) {
        return testPermission(sender);
    }

    /**
     * Registers the command
     * @param fallbackPrefix A prefix which is prepended to the command
     *                       with a ':' one or more times to make the command unique
     */
    @SneakyThrows({ReflectiveOperationException.class})
    public void register(String fallbackPrefix) {
        Server server = Bukkit.getServer();
        Class<? extends Server> serverClass = server.getClass();

        Method method = serverClass.getMethod("getCommandMap");
        CommandMap commandMap = (CommandMap) method.invoke(server);
        commandMap.register(fallbackPrefix,this);
    }

}
