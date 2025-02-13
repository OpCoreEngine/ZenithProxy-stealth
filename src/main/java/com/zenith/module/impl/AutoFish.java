package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.cache.data.entity.Entity;
import com.zenith.cache.data.entity.EntityStandard;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.module.ClientBotTick;
import com.zenith.event.module.EntityFishHookSpawnEvent;
import com.zenith.event.module.SplashSoundEffectEvent;
import com.zenith.feature.world.*;
import com.zenith.mc.item.ItemRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ProjectileData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;

import java.time.Instant;
import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class AutoFish extends AbstractInventoryModule {
    private int fishHookEntityId = -1;
    private Hand rodHand = Hand.MAIN_HAND;
    private int delay = 0;
    public static final int MOVEMENT_PRIORITY = 10;
    private Instant castTime = Instant.EPOCH;

    public AutoFish() {
        super(HandRestriction.EITHER, 2, MOVEMENT_PRIORITY);
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(EntityFishHookSpawnEvent.class, this::handleEntityFishHookSpawnEvent),
            of(SplashSoundEffectEvent.class, this::handleSplashSoundEffectEvent),
            of(ClientBotTick.class, this::handleClientTick),
            of(ClientBotTick.Starting.class, this::handleBotTickStarting),
            of(ClientBotTick.Stopped.class, this::handleBotTickStopped)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoFish.enabled;
    }

    public void handleBotTickStarting(final ClientBotTick.Starting event) {
        reset();
    }

    public void handleBotTickStopped(final ClientBotTick.Stopped event) {
        reset();
    }

    private synchronized void reset() {
        fishHookEntityId = -1;
        delay = 0;
        castTime = Instant.EPOCH;
    }

    public void handleEntityFishHookSpawnEvent(final EntityFishHookSpawnEvent event) {
        try {
            if (event.getOwnerEntityId() != CACHE.getPlayerCache().getEntityId()) return;
            fishHookEntityId = event.fishHookObject().getEntityId();
        } catch (final Exception e) {
            error("Failed to handle EntityFishHookSpawnEvent", e);
        }
    }

    public void handleSplashSoundEffectEvent(final SplashSoundEffectEvent event) {
        if (isFishing()) {
            // reel in
            requestUseRod(false);
            delay = 20;
            fishHookEntityId = -1;
        }
    }

    public void handleClientTick(final ClientBotTick event) {
        if (delay > 0) {
            delay--;
            return;
        }
        if (MODULE.get(AutoEat.class).isEating() || MODULE.get(KillAura.class).isActive()) return;
        if (!isFishing()
            && switchToFishingRod()
            && isRodInHand()) {
            requestUseRod(true).addInputExecutedListener(future -> {
                if (future.getClickResult() instanceof ClickResult.RightClickResult leftClickResult) {
                    if (leftClickResult.getType() == ClickResult.RightClickResult.RightClickType.USE_ITEM) {
                        castTime = Instant.now();
                        delay = 5;
                    }
                }
            });
        }
        if (isFishing() && Instant.now().getEpochSecond() - castTime.getEpochSecond() > 60) {
            // something's wrong, probably don't have hook in water
            warn("Probably don't have hook in water. reeling in");
            fishHookEntityId = -1;
            requestUseRod(false);
        }
    }

    private boolean isRodInHand() {
        return switch (rodHand) {
            case MAIN_HAND -> itemPredicate(CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND));
            case OFF_HAND -> itemPredicate(CACHE.getPlayerCache().getEquipment(EquipmentSlot.OFF_HAND));
            case null -> false;
        };
    }

    private InputRequestFuture requestUseRod(boolean cast) {
        return INPUTS.submit(InputRequest.builder()
                          .input(Input.builder()
                                     .rightClick(true)
                                     .clickTarget(ClickTarget.None.INSTANCE)
                                     .hand(rodHand)
                                     .clickRequiresRotation(cast)
                                     .build())
                          .yaw(CONFIG.client.extra.autoFish.yaw)
                          .pitch(CONFIG.client.extra.autoFish.pitch)
                          .priority(MOVEMENT_PRIORITY)
                          .build());
    }

    public boolean switchToFishingRod() {
        delay = doInventoryActions();
        if (getHand() != null && delay == 0) {
            rodHand = getHand();
            return true;
        }
        return false;
    }

    private boolean isFishing() {
        final Entity cachedEntity = CACHE.getEntityCache().get(fishHookEntityId);
        return cachedEntity instanceof EntityStandard standard
            && standard.getEntityType() == EntityType.FISHING_BOBBER
            && ((ProjectileData) standard.getObjectData()).getOwnerId() == CACHE.getPlayerCache().getEntityId();
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        return itemStack != Container.EMPTY_STACK && itemStack.getId() == ItemRegistry.FISHING_ROD.id();
    }
}
