package com.zenith.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandUsage;
import com.zenith.module.impl.AutoSolver;

import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;
import static com.zenith.Globals.*;
import static java.util.Arrays.asList;

public class AutoSolverCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("autoSolver")
            .category(CommandCategory.MODULE)
            .description("Automatically solves simple puzzles like Kontrol windows and sign inputs")
            .usageLines(asList(
                "on/off",
                "help"
            ))
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoSolver")
            .then(argument("toggle", toggle()).executes(c -> {
                CONFIG.client.extra.autoSolver.enabled = getToggle(c, "toggle");
                MODULE.get(AutoSolver.class).syncEnabledFromConfig();
                c.getSource().getEmbed()
                    .title("AutoSolver " + toggleStrCaps(CONFIG.client.extra.autoSolver.enabled));
                return OK;
            }))
            .executes(c -> {
                c.getSource().getEmbed()
                    .title("AutoSolver is currently " + toggleStrCaps(CONFIG.client.extra.autoSolver.enabled));
                return OK;
            });
    }

}