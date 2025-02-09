package com.zenith.module.impl;

import com.zenith.Proxy;
import com.zenith.event.module.AutoEatOutOfFoodEvent;
import com.zenith.event.module.ClientBotTick;
import com.zenith.feature.world.InputRequest;
import com.zenith.mc.food.FoodData;
import com.zenith.mc.food.FoodRegistry;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemPacket;

import java.time.Duration;
import java.time.Instant;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.*;

public class AutoEat extends AbstractInventoryModule {
    private int delay = 0;
    private Instant lastAutoEatOutOfFoodWarning = Instant.EPOCH;
    private boolean isEating = false;
    private static final int MOVEMENT_PRIORITY = 1000;

    public AutoEat() {
        super(HandRestriction.EITHER, 0, MOVEMENT_PRIORITY);
    }

    @Override
    public void subscribeEvents() {
        EVENT_BUS.subscribe(
            this,
            of(ClientBotTick.class, this::handleClientTick),
            of(ClientBotTick.Starting.class, this::handleBotTickStarting)
        );
    }

    public boolean isEating() {
        return enabledSetting() && isEating;
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoEat.enabled;
    }

    public void handleClientTick(final ClientBotTick e) {
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
                && playerHealthBelowThreshold()
                && Proxy.getInstance().getOnlineTimeSeconds() > 10) {
            if (delay > 0) {
                delay--;
                return;
            } else {
                if (switchToFood()) {
                    startEating();
                }
            }
            if (isEating) {
                INPUTS.submit(InputRequest.builder()
                                  .priority(MOVEMENT_PRIORITY)
                                  .build());
            }
        } else {
            isEating = false;
        }
    }

    public boolean switchToFood() {
        delay = doInventoryActions();
        final boolean shouldStartEating = getHand() != null && delay == 0;
        final boolean notHoldingFoodAndNotSwapping = getHand() == null && delay == 0;
        final boolean holdingFoodOrSwapping = getHand() != null || delay != 0;
        if (notHoldingFoodAndNotSwapping) {
            if (CONFIG.client.extra.autoEat.warning && Instant.now().minus(Duration.ofHours(7)).isAfter(lastAutoEatOutOfFoodWarning)) {
                CLIENT_LOG.warn("[AutoEat] Out of food");
                EVENT_BUS.postAsync(new AutoEatOutOfFoodEvent());
                lastAutoEatOutOfFoodWarning = Instant.now();
            }
        }
        isEating = holdingFoodOrSwapping;
        return shouldStartEating;
    }

    public void startEating() {
        var hand = getHand();
        if (hand == null) return;
        isEating = true;
        delay = 50;
        sendClientPacketAsync(new ServerboundUseItemPacket(hand, CACHE.getPlayerCache().getActionId().incrementAndGet(), CACHE.getPlayerCache().getYaw(), CACHE.getPlayerCache().getPitch()));
    }

    public void handleBotTickStarting(final ClientBotTick.Starting event) {
        delay = 0;
        lastAutoEatOutOfFoodWarning = Instant.EPOCH;
        isEating = false;
    }

    private boolean playerHealthBelowThreshold() {
        return CACHE.getPlayerCache().getThePlayer().getHealth() <= CONFIG.client.extra.autoEat.healthThreshold
            || CACHE.getPlayerCache().getThePlayer().getFood() <= CONFIG.client.extra.autoEat.hungerThreshold;
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        FoodData foodData = FoodRegistry.REGISTRY.get(itemStack.getId());
        return foodData != null && foodData.isSafeFood();
    }
}
