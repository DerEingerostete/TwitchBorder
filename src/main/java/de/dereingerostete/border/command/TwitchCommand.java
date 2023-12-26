package de.dereingerostete.border.command;

import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.chat.Chat;
import de.dereingerostete.border.chat.Logging;
import de.dereingerostete.border.command.util.SimpleCommand;
import de.dereingerostete.border.entity.twitch.TwitchCredentials;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class TwitchCommand extends SimpleCommand {

    public TwitchCommand() {
        super("twitch", true);
        setDescription("Authenticates with twitch");
        setPermission("twitchborder.authenticate");
        setUsage("/" + getName() + " or /" + getName() + " <URL>");
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args, int arguments) {
        if (arguments == 0) {
            create(sender);
        } else {
            login(sender, args[0]);
        }
    }

    protected void create(@NotNull CommandSender sender) {
        String link = BorderPlugin.getCredentials().createAuthURL();
        TextComponent linkComponent = new TextComponent("Link");
        linkComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
        linkComponent.setColor(ChatColor.BLUE);
        linkComponent.setUnderlined(true);

        Text hoverText = new Text("§7Click to open");
        linkComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverText));

        TextComponent component = new TextComponent(Chat.getPrefix());
        component.addExtra("§7Click on the ");
        component.addExtra(linkComponent);
        component.addExtra("§7 to authenticate");
        sender.spigot().sendMessage(component);

        Chat.toPlayer(sender, "§7After authenticating type /" + getName() +
                " with the URL shown in the browser (The url starts with http://localhost)");
    }

    protected void login(@NotNull CommandSender sender, @NotNull String input) {
        int paramStartIndex = input.indexOf("/?");
        if (paramStartIndex == -1) {
            Chat.toPlayer(sender, "§cCould not parse URL. Check if the URL includes http://localhost/?");
            return;
        }

        Map<String, String> parameters = new HashMap<>();
        String paramString = input.substring(paramStartIndex + 2);

        String[] queryParts = paramString.split("&");
        for (String part : queryParts) {
            int index = part.indexOf('=');
            String key = part.substring(0, index);
            String value = part.substring(index + 1);
            parameters.put(key, URLDecoder.decode(value, StandardCharsets.UTF_8));
        }

        TwitchCredentials credentials = BorderPlugin.getCredentials();
        String actualState = credentials.getState();
        if (actualState == null) {
            Chat.toPlayer(sender, "§cNo authentication has been started. Use /twitch");
            return;
        }

        String state = parameters.get("state");
        if (state == null || !state.equals(actualState)) {
            Chat.toPlayer(sender, "§cInvalid state! Authentication blocked");
            return;
        }

        String code = parameters.get("code");
        if (code == null) {
            Chat.toPlayer(sender, "§cAuthentication failed! Please try again");
            return;
        }

        try {
            String displayName = credentials.authenticate(code);
            Chat.toPlayer(sender, "§7Successfully authenticated with §5Twitch §7as §5" + displayName);
        } catch (IOException exception) {
            Logging.warning("Failed to authenticate with Twitch", exception);
        }
    }

}