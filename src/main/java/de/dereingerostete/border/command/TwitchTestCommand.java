package de.dereingerostete.border.command;

import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.common.enums.SubscriptionType;
import com.github.twitch4j.pubsub.domain.ChannelBitsData;
import com.github.twitch4j.pubsub.domain.FollowingData;
import com.github.twitch4j.pubsub.domain.SubGiftData;
import com.github.twitch4j.pubsub.domain.SubscriptionData;
import com.github.twitch4j.pubsub.events.ChannelBitsEvent;
import com.github.twitch4j.pubsub.events.ChannelSubGiftEvent;
import com.github.twitch4j.pubsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.pubsub.events.FollowingEvent;
import de.dereingerostete.border.BorderPlugin;
import de.dereingerostete.border.chat.Chat;
import de.dereingerostete.border.command.util.SimpleCommand;
import de.dereingerostete.border.entity.twitch.TwitchIntegration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class TwitchTestCommand extends SimpleCommand {

    public TwitchTestCommand() {
        super("twitchtest", true);
        setDescription("Used for testing of Twitch Events");
        setPermission("twitch.test");
        setUsage("/" + getName() + " <gift/subs/follow> <amount>");
    }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args, int arguments) {
        TwitchIntegration integration = BorderPlugin.getIntegration();
        if (integration == null) {
            Chat.toPlayer(sender, "§cTwitch is not connected. Please authenticate first!");
            return;
        }

        if (arguments == 0) {
            Chat.toPlayer(sender, "§cWrong usage!§7 Use " + getUsage());
            return;
        }

        String type = args[0];
        switch (type.toLowerCase()) {
            case "follow" -> handleFollow(sender);
            case "gift" -> handleGift(sender, args);
            case "sub" -> handleSubscription(sender, args);
            case "bits" -> handleBits(sender, args);
        }
    }

    protected void handleSubscription(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        if (args.length != 2) {
            Chat.toPlayer(sender, "§cNo tier is provided!");
            return;
        }

        SubscriptionPlan tier;
        try {
            tier = SubscriptionPlan.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException exception) {
            List<String> names = Arrays.stream(SubscriptionPlan.values())
                    .map(SubscriptionPlan::name).toList();
            Chat.toPlayer(sender, "§cThis tier does not exists! Tiers are: " + names);
            return;
        }

        SubscriptionData data = new SubscriptionData();
        data.setDisplayName(getRandomUsername());
        data.setContext(SubscriptionType.RESUB);
        data.setIsGift(false);
        data.setSubPlanName(tier.name());
        data.setSubPlan(tier);
        ChannelSubscribeEvent event = new ChannelSubscribeEvent(data);

        TwitchIntegration integration = Objects.requireNonNull(BorderPlugin.getIntegration());
        integration.onSubscribe(event);
        Chat.toPlayer(sender, "§7Created dummy gift event");
    }

    protected void handleBits(@NotNull CommandSender sender, @NotNull String[] args) {
        int amount = getAmount(sender, args);
        if (amount == -1) return;

        ChannelBitsData data = new ChannelBitsData();
        data.setBitsUsed(amount);
        data.setUserId("dereingerostete");
        data.isAnonymous(false);
        ChannelBitsEvent event = new ChannelBitsEvent(data);

        TwitchIntegration integration = Objects.requireNonNull(BorderPlugin.getIntegration());
        integration.onBits(event);
        Chat.toPlayer(sender, "§7Created dummy gift event");
    }

    protected void handleGift(@NotNull CommandSender sender, @NotNull String[] args) {
        int amount = getAmount(sender, args);
        if (amount == -1) return;

        if (args.length != 3) {
            Chat.toPlayer(sender, "§cNo tier is provided!");
            return;
        }

        SubscriptionPlan tier;
        try {
            tier = SubscriptionPlan.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException exception) {
            List<String> names = Arrays.stream(SubscriptionPlan.values())
                    .map(SubscriptionPlan::name).toList();
            Chat.toPlayer(sender, "§cThis tier does not exists! Tiers are: " + names);
            return;
        }

        SubGiftData data = new SubGiftData();
        data.setCount(amount);
        data.setDisplayName(getRandomUsername());
        data.setTier(tier);

        ChannelSubGiftEvent event = new ChannelSubGiftEvent(data);
        TwitchIntegration integration = Objects.requireNonNull(BorderPlugin.getIntegration());
        integration.onSubGift(event);
        Chat.toPlayer(sender, "§7Created dummy gift event");
    }

    protected void handleFollow(@NotNull CommandSender sender) {
        TwitchIntegration integration = Objects.requireNonNull(BorderPlugin.getIntegration());

        FollowingData data = new FollowingData();
        data.setUserId("TEST_USER_ID");
        data.setUsername("TEST_USERNAME");
        data.setDisplayName(getRandomUsername());

        FollowingEvent event = new FollowingEvent("TEST_CHANNEL_ID", data);
        integration.onFollow(event);
        Chat.toPlayer(sender, "§7Created dummy follow event");
    }

    protected int getAmount(@NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        try {
            if (args.length < 2) {
                Chat.toPlayer(sender, "§cYou need to enter an amount");
                return -1;
            }

            int amount = Integer.parseInt(args[1]);
            if (amount < 1) {
                Chat.toPlayer(sender, "§cThe amount must be greater than one");
                return -1;
            } else return amount;
        } catch (NumberFormatException exception) {
            Chat.toPlayer(sender, "§cThe amount must be a number");
            return -1;
        }
    }

    @NotNull
    protected String getRandomUsername() {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://public-sonjj.p.rapidapi.com/identity?locale=en_US&gender=null&" +
                            "key=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.IntcImRhdGFcIjp7XCJsb2NhbGVcIj" +
                            "pcImVuX1VTXCIsXCJnZW5kZXJcIjpcImZlbWFsZVwiLFwibWluQWdlXCI6XCIxOFwiLFwibWF4Q" +
                            "WdlXCI6XCI3MFwiLFwiZG9tYWluXCI6XCJ1Z2VuZXIuY29tXCJ9LFwiY3JlYXRlZF9hdFwiOjE2O" +
                            "DIyNTE5ODV9Ig.xbwrkP6V3bW8XQYh4mrM2n71zhtTSE6MKYbeK7uLiRk&rapidapi-key=f871a2" +
                            "2852mshc3ccc49e34af1e8p126682jsn734696f1f081")
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (body == null) return "DerEingerostete";

            String responseString = body.string();
            JSONObject object = new JSONObject(responseString).getJSONObject("items");
            String username = object.getString("username");

            response.close();
            return username;
        } catch (Exception exception) {
            return "RandomUser";
        }
    }

}