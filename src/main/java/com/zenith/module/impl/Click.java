package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.player.Input;
import com.zenith.feature.player.InputRequest;
import com.zenith.module.api.Module;
import com.zenith.util.timer.Timer;
import com.zenith.util.timer.Timers;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;

import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.INPUTS;

public class Click extends Module {

    public static final int MOVEMENT_PRIORITY = 501;

    private final Timer holdRightClickTimer = Timers.tickTimer();
    private final Timer holdLeftClickTimer = Timers.tickTimer();
    private Hand holdRightClickLastHand = Hand.MAIN_HAND;

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::onClientBotTick)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.click.enabled;
    }

    private void onClientBotTick(ClientBotTick event) {
        if (CONFIG.client.extra.click.holdLeftClick) {
            if (holdLeftClickTimer.tick(CONFIG.client.extra.click.holdLeftClickInterval)) {
                var req = InputRequest.builder().priority(MOVEMENT_PRIORITY);
                var in = Input.builder().leftClick(true);
                if (CONFIG.client.extra.click.hasRotation) {
                    req.yaw(CONFIG.client.extra.click.rotationYaw)
                        .pitch(CONFIG.client.extra.click.rotationPitch);
                }
                in.sneaking(CONFIG.client.extra.click.holdSneak);
                INPUTS.submit(req
                    .owner(this)
                    .input(in.build())
                    .build());
            } else if (CONFIG.client.extra.click.holdSneak) {
                INPUTS.submit(InputRequest.builder()
                    .owner(this)
                    .input(Input.builder()
                        .sneaking(true)
                        .build())
                    .priority(0) // 0 priority allows other modules to override this if needed
                    .build());
            }
        } else if (CONFIG.client.extra.click.holdRightClick) {
            if (holdRightClickTimer.tick(CONFIG.client.extra.click.holdRightClickInterval)) {
                var req = InputRequest.builder().owner(this).priority(MOVEMENT_PRIORITY);
                var in = Input.builder().rightClick(true);
                Hand hand = switch (CONFIG.client.extra.click.holdRightClickMode) {
                    case MAIN_HAND -> Hand.MAIN_HAND;
                    case OFF_HAND -> Hand.OFF_HAND;
                    case ALTERNATE_HANDS -> holdRightClickLastHand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
                };
                holdRightClickLastHand = hand;
                in.hand(hand);
                if (CONFIG.client.extra.click.hasRotation) {
                    req.yaw(CONFIG.client.extra.click.rotationYaw)
                        .pitch(CONFIG.client.extra.click.rotationPitch);
                }
                in.sneaking(CONFIG.client.extra.click.holdSneak);
                INPUTS.submit(req
                    .input(in.build())
                    .build());
            } else if (CONFIG.client.extra.click.holdSneak) {
                INPUTS.submit(InputRequest.builder()
                    .owner(this)
                    .input(Input.builder()
                        .sneaking(true)
                        .build())
                    .priority(0) // 0 priority allows other modules to override this if needed
                    .build());
            }
        }
    }
}
