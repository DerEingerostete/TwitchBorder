package de.dereingerostete.border.entity.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.philippheuer.events4j.reactor.ReactorEventHandler;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.common.events.TwitchEvent;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.ChannelSearchList;
import com.github.twitch4j.helix.domain.ChannelSearchResult;
import com.github.twitch4j.pubsub.TwitchPubSub;
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
import de.dereingerostete.border.chat.Logging;
import de.dereingerostete.border.entity.BorderManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwitchIntegration {
    private final @NotNull Map<Class<? extends TwitchEvent>, BorderGrowData> eventDataMap;
    private final @NotNull DecimalFormat decimalFormat;
    private final @NotNull TwitchClient client;

    public TwitchIntegration() {
        this.decimalFormat = new DecimalFormat("#.##");
        decimalFormat.setRoundingMode(RoundingMode.UP);

        client = TwitchClientBuilder.builder()
                .withEnablePubSub(true)
                .withDefaultEventHandler(ReactorEventHandler.class)
                .withEnableHelix(true)
                .build();

        TwitchCredentials credentials = BorderPlugin.getCredentials();
        credentials.getOAuthLogins().forEach(this::addChannel);

        EventManager eventManager = client.getEventManager();
        eventManager.onEvent(ChannelBitsEvent.class, this::onBits);
        eventManager.onEvent(ChannelSubGiftEvent.class, this::onSubGift);
        eventManager.onEvent(FollowingEvent.class, this::onFollow);
        eventManager.onEvent(ChannelSubscribeEvent.class, this::onSubscribe);

        this.eventDataMap = new HashMap<>();
        eventDataMap.put(ChannelBitsEvent.class, new BorderGrowData(BorderGrowData.Type.BITS));
        eventDataMap.put(ChannelSubGiftEvent.class, new BorderGrowData(BorderGrowData.Type.GIFTED_SUBSCRIPTION));
        eventDataMap.put(FollowingEvent.class, new BorderGrowData(BorderGrowData.Type.FOLLOW));
        eventDataMap.put(ChannelSubscribeEvent.class, new BorderGrowData(BorderGrowData.Type.SUBSCRIPTION));
    }

    public void addChannel(@NotNull OAuthData data) {
        OAuth2Credential credential = data.toCredential();
        String channelId = data.getChannelId();

        TwitchPubSub pubSub = client.getPubSub();
        pubSub.listenForCheerEvents(credential, channelId);
        pubSub.listenForChannelSubGiftsEvents(credential, channelId);
        pubSub.listenForFollowingEvents(credential, channelId);
        pubSub.listenForSubscriptionEvents(credential, channelId);

        String displayName = data.getDisplayName();
        Logging.info("Added listeners for events on channel " + channelId + "/" + displayName);
    }

    public void close() {
        client.close();
    }

    public void onSubscribe(@NotNull ChannelSubscribeEvent event) {
        SubscriptionData data = event.getData();
        if (data.getIsGift()) return; //Ignore gifts

        int tierCount = getSubMultiplier(data.getSubPlan());
        BorderGrowData growData = eventDataMap.get(event.getClass());
        double growAmount = growData.getGrowFunction().apply(tierCount);

        BorderManager borderManager = BorderPlugin.getBorderManager();
        borderManager.increase(growAmount);

        if (growData.showFirework(tierCount)) {
            growData.spawnFirework();
        }

        String name = data.getDisplayName();
        String tierName = fromPlan(data.getSubPlan());
        Chat.broadcast("§5" + name + "§7 subscribed at §5" + tierName + "!");
        Chat.broadcast("The world border will grow §5" + decimalFormat.format(growAmount) + " blocks");
    }

    public void onSubGift(@NotNull ChannelSubGiftEvent event) {
        SubGiftData data = event.getData();
        String name = data.getDisplayName();
        SubscriptionPlan plan = data.getTier();
        int count = data.getCount();

        if (plan == SubscriptionPlan.NONE) {
            Logging.warning("Failed to handle sub gift event: Plan is NONE");
            return;
        }

        int planMultiplier = getSubMultiplier(plan);
        String tierName = fromPlan(plan);

        int multipliedCount = count * planMultiplier;
        BorderGrowData growData = eventDataMap.get(event.getClass());
        double growAmount = growData.getGrowFunction().apply(multipliedCount);

        BorderManager borderManager = BorderPlugin.getBorderManager();
        borderManager.increase(growAmount);

        if (growData.showFirework(multipliedCount)) {
            growData.spawnFirework();
        }

        Chat.broadcast("§5" + name + "§7 gifted §5" + count + ' ' + tierName + " subs!");
        Chat.broadcast("The world border will grow §5" + decimalFormat.format(growAmount) + " blocks");
    }

    private int getSubMultiplier(@NotNull SubscriptionPlan plan) {
        return switch (plan) {
            case TIER1, TWITCH_PRIME -> 1;
            case TIER2 -> 2;
            case TIER3 -> 5;
            default -> throw new IllegalStateException("Unexpected value: " + plan);
        };
    }

    public void onFollow(@NotNull FollowingEvent event) {
        FollowingData data = event.getData();
        String displayName = data.getDisplayName();

        BorderGrowData growData = eventDataMap.get(event.getClass());
        double growAmount = growData.getGrowFunction().apply(1);

        BorderManager borderManager = BorderPlugin.getBorderManager();
        borderManager.increase(growAmount);

        if (growData.showFirework(1)) {
            growData.spawnFirework();
        }

        Chat.broadcast("§5" + displayName + "§7 followed!");
        Chat.broadcast("The world border will grow §5" + decimalFormat.format(growAmount) + " blocks");
    }

    public void onBits(@NotNull ChannelBitsEvent event) {
        ChannelBitsData data = event.getData();
        int bits = data.getBitsUsed();

        String name = "Anonymous";
        String userId = data.getUserId();
        if (!data.isAnonymous() && userId != null) {
            String displayName = getDisplayNameById(userId);
            if (displayName != null) name = displayName;
        }

        BorderGrowData growData = eventDataMap.get(event.getClass());
        double growAmount = growData.getGrowFunction().apply(bits);

        BorderManager borderManager = BorderPlugin.getBorderManager();
        borderManager.increase(growAmount);

        if (growData.showFirework(bits)) {
            growData.spawnFirework();
        }

        Chat.broadcast("§5" + name + "§7 cheered §5" + bits + " bits!");
        Chat.broadcast("The world border will grow §5" + decimalFormat.format(growAmount) + " blocks");
    }

    @Nullable
    private String getDisplayNameById(@NotNull String userId) {
        TwitchHelix helix = client.getHelix();
        List<OAuthData> oAuthDataList = BorderPlugin.getCredentials().getOAuthLogins();

        if (oAuthDataList.isEmpty()) {
            Logging.warning("Failed to get display name: Credential is null");
            return null;
        }

        String authToken = oAuthDataList.get(0).getAccessToken();
        ChannelSearchList list = helix.searchChannels(authToken, userId, 1, null, false).execute();
        List<ChannelSearchResult> results = list.getResults();

        if (results.isEmpty()) return null;
        else return results.get(0).getDisplayName();
    }

    @NotNull
    private String fromPlan(@NotNull SubscriptionPlan plan) {
        return switch (plan) {
            case TIER1 -> "Tier 1";
            case TIER2 -> "Tier 2";
            case TIER3 -> "Tier 3";
            default -> "Tier ?";
        };
    }

}