package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.inventory.InventoryActionRequest;
import com.zenith.feature.player.ClickTarget;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.mc.block.Direction;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class AutoOmen extends AbstractInventoryModule {
    private int delay = 0;
    private boolean isEating = false;
    public static final int MOVEMENT_PRIORITY = 600;
    private static final List<Effect> OMEN_EFFECTS = List.of(
        Effect.BAD_OMEN,
        Effect.RAID_OMEN,
        Effect.TRIAL_OMEN
    );
    private long lastHadOmen = 0L;
    private long lastRaidActive = 0L;

    public AutoOmen() {
        super(HandRestriction.EITHER, 3, MOVEMENT_PRIORITY);
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoOmen.enabled;
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleClientTick),
            of(ClientBotTick.Starting.class, this::handleBotTickStarting)
        );
    }

    public void handleClientTick(final ClientBotTick e) {
        if (hasOmenEffect()) {
            lastHadOmen = System.currentTimeMillis();
        }
        if (isRaidActive()) {
            lastRaidActive = System.currentTimeMillis();
        }
        if (CACHE.getPlayerCache().getThePlayer().isAlive()
            && (CONFIG.client.extra.autoOmen.whileRaidActive || (System.currentTimeMillis() - lastRaidActive > CONFIG.client.extra.autoOmen.raidCooldownMs))
            && (CONFIG.client.extra.autoOmen.whileOmenActive || (System.currentTimeMillis() - lastHadOmen > CONFIG.client.extra.autoOmen.omenCooldownMs))
            && CACHE.getPlayerCache().getGameMode() != GameMode.CREATIVE
            && CACHE.getPlayerCache().getGameMode() != GameMode.SPECTATOR
            && Proxy.getInstance().getOnlineTimeSeconds() > 1) {
            if (delay > 0) {
                delay--;
                if (isEating) {
                    INPUTS.submit(InputRequest.noInput(this, MOVEMENT_PRIORITY));
                    INVENTORY.submit(InventoryActionRequest.noAction(this, MOVEMENT_PRIORITY));
                }
                return;
            }
            if (switchToFood()) {
                startEating();
            }
        } else {
            if (isEating) {
                if (delay > 0) { // we got interrupted during drinking
                    // todo: have bot automatically cancel eats if not confirmed every tick?
                    sendClientPacketAsync(new ServerboundPlayerActionPacket(
                        PlayerAction.RELEASE_USE_ITEM,
                        0, 0, 0,
                        Direction.DOWN.mcpl(),
                        CACHE.getPlayerCache().getSeqId().incrementAndGet()
                    ));
                    debug("Got interrupted during omen drink");
                    delay = 0;
                } else {
                    delay = 20;
                    debug("Omen drink completed");
                }
            } else {
                delay = 0;
            }
            isEating = false;
        }
    }

    public boolean switchToFood() {
        delay = doInventoryActions();
        final boolean shouldStartEating = getHand() != null && delay == 0;
        isEating = getHand() != null || delay != 0;
        return shouldStartEating;
    }

    public void startEating() {
        var hand = getHand();
        if (hand == null) return;
        INPUTS.submit(InputRequest.builder()
                .owner(this)
                .input(Input.builder()
                    .rightClick(true)
                    .clickTarget(ClickTarget.None.INSTANCE)
                    .build())
                .priority(MOVEMENT_PRIORITY)
                .build())
            .addInputExecutedListener(future -> {
                debug("Drinking Omen");
                isEating = true;
                delay = 50;
            });
    }

    public void handleBotTickStarting(final ClientBotTick.Starting event) {
        delay = 0;
        isEating = false;
    }

    @Override
    public boolean itemPredicate(final ItemStack itemStack) {
        ItemData itemData = ItemRegistry.REGISTRY.get(itemStack.getId());
        return itemData != null && itemData == ItemRegistry.OMINOUS_BOTTLE;
    }

    private boolean hasOmenEffect() {
        for (int i = 0; i < OMEN_EFFECTS.size(); i++) {
            if (CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().containsKey(OMEN_EFFECTS.get(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRaidActive() {
        for (var bossBar : CACHE.getBossBarCache().getBossBars().values()) {
            if (isRaidActiveComponent(bossBar.getTitle())) return true;
        }
        return false;
    }

    private boolean isRaidActiveComponent(final Component component) {
        if (component instanceof TranslatableComponent translatableComponent) {
            var key = translatableComponent.key();
            return key.startsWith("event.minecraft.raid") && !key.contains("victory");
        } else {
            for (var child : component.children()) {
                if (isRaidActiveComponent(child)) {
                    return true;
                }
            }
        }
        return false;
    }
}
