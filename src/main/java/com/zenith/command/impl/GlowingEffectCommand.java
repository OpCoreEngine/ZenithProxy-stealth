package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.module.impl.GlowingEffect;

import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class GlowingEffectCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("glowing")
            .category(CommandCategory.MODULE)
            .description("""
            Makes all players within view distance appear to have the glowing effect.
            
            This is similar to the spectral arrow effect that makes players glow through walls.
            """)
            .usageLines(
                "on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("glowing")
            .then(argument("toggle", toggle())
                .executes(c -> {
                    CONFIG.client.extra.glowingEffect.enabled = getToggle(c, "toggle");
                    MODULE.get(GlowingEffect.class).syncEnabledFromConfig();
                    c.getSource().getEmbed()
                        .title("Glowing Effect " + toggleStrCaps(CONFIG.client.extra.glowingEffect.enabled));
                    return OK;
                }))
            .executes(c -> {
                c.getSource().getEmbed()
                    .title("Glowing effect is currently " + toggleStrCaps(CONFIG.client.extra.glowingEffect.enabled));
                return OK;
            });
    }
}