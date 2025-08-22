package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;
import com.zenith.module.impl.AutoFish;
import com.zenith.util.math.MathHelper;
import org.cloudburstmc.math.vector.Vector2d;

import static com.zenith.Globals.*;
import static com.zenith.command.brigadier.RotationArgument.getRotation;
import static com.zenith.command.brigadier.RotationArgument.rotation;
import static com.mojang.brigadier.arguments.FloatArgumentType.floatArg;
import static com.mojang.brigadier.arguments.FloatArgumentType.getFloat;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoFishCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("autoFish")
            .category(CommandCategory.MODULE)
            .description("""
             Automatically fishes, both casting and reeling.
             
             AutoFishing will prevent you from being AFK kicked. It's recommended to disable AntiAFK.
             """)
            .usageLines(
                "on/off",
                "rotation <yaw> <pitch>",
                "rotation sync",
                "randomYawPitch on/off",
                "randomRange <yaw> <pitch>",
                "pauseOnContainer on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoFish")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoFish.enabled = getToggle(c, "toggle");
                MODULE.get(AutoFish.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoFish " + toggleStr(CONFIG.client.extra.autoFish.enabled));
                return OK;
            }))
            .then(literal("rotation")
                .then(literal("sync").executes(c -> {
                    // normalize yaw and pitch to -180 to 180 and -90 to 90
                    CONFIG.client.extra.autoFish.yaw = MathHelper.wrapYaw(CACHE.getPlayerCache().getYaw());
                    CONFIG.client.extra.autoFish.pitch = MathHelper.wrapPitch(CACHE.getPlayerCache().getPitch());
                    c.getSource().getEmbed()
                        .title("Rotation synced to player!");
                }))
                .then(argument("rot", rotation()).executes(c -> {
                    Vector2d rot = getRotation(c, "rot");
                    CONFIG.client.extra.autoFish.yaw = (float) rot.getX();
                    CONFIG.client.extra.autoFish.pitch = (float) rot.getY();
                    c.getSource().getEmbed()
                        .title("Rotation set to " + CONFIG.client.extra.autoFish.yaw + " " + CONFIG.client.extra.autoFish.pitch);
                })))
            .then(literal("randomYawPitch")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.autoFish.randomYawPitch = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Random Yaw/Pitch " + toggleStr(CONFIG.client.extra.autoFish.randomYawPitch));
                    return OK;
                })))
            .then(literal("randomRange")
                .then(argument("yawRange", floatArg(0, 45)).then(argument("pitchRange", floatArg(0, 20)).executes(c -> {
                    CONFIG.client.extra.autoFish.randomYawRange = getFloat(c, "yawRange");
                    CONFIG.client.extra.autoFish.randomPitchRange = getFloat(c, "pitchRange");
                    c.getSource().getEmbed()
                        .title("Random Range")
                        .addField("Yaw Range", "±" + CONFIG.client.extra.autoFish.randomYawRange, true)
                        .addField("Pitch Range", "±" + CONFIG.client.extra.autoFish.randomPitchRange, true);
                    return OK;
                }))))
            .then(literal("pauseOnContainer")
                .then(argument("toggle", toggle()).executes(c -> {
                    CONFIG.client.extra.autoFish.pauseOnContainerOpen = getToggle(c, "toggle");
                    c.getSource().getEmbed()
                        .title("Pause on Container Open " + toggleStr(CONFIG.client.extra.autoFish.pauseOnContainerOpen));
                    return OK;
                })));
    }

    @Override
    public void defaultEmbed(final Embed builder) {
        builder
            .addField("AutoFish", toggleStr(CONFIG.client.extra.autoFish.enabled), false)
            .addField("Yaw", CONFIG.client.extra.autoFish.yaw, false)
            .addField("Pitch", CONFIG.client.extra.autoFish.pitch, false)
            .addField("Random Yaw/Pitch", toggleStr(CONFIG.client.extra.autoFish.randomYawPitch), false)
            .addField("Random Yaw Range", "±" + CONFIG.client.extra.autoFish.randomYawRange, false)
            .addField("Random Pitch Range", "±" + CONFIG.client.extra.autoFish.randomPitchRange, false)
            .addField("Pause on Container", toggleStr(CONFIG.client.extra.autoFish.pauseOnContainerOpen), false)
            .primaryColor();
    }
}
