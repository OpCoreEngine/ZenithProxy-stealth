package com.zenith.docs;

import com.zenith.Globals;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class CommandDocsGenerator {

    @Test
    public void generateDocs() {
        var outputFile = new File("build/Commands.md");
        if (outputFile.exists()) {
            outputFile.delete();
        }
        final StringBuilder sb = new StringBuilder();
        sb.append("# ZenithProxy Commands Documentation\n\n");
        sb.append("## Command Prefixes\n\n");
        sb.append("#### Discord\n\n");
        sb.append("`.` (e.g. `.help`)").append("\n\n");
        sb.append("#### In-game\n\n");
        sb.append("`/` OR `!` (e.g. `/help`)").append("\n\n");
        sb.append("#### Terminal\n\n");
        sb.append("N/A (e.g. `help`)").append("\n\n");
        var categoryOrder = asList(CommandCategory.CORE, CommandCategory.MANAGE, CommandCategory.INFO, CommandCategory.MODULE);

        var commands = Globals.COMMAND.getCommands().stream()
            .map(Command::commandUsage)
            .toList();
        categoryOrder.forEach(category -> {
            sb.append("## ").append(category.getName()).append(" Commands\n\n");
            commands.stream()
                .filter(command -> command.getCategory() == category)
                .forEachOrdered(command -> {
                    sb.append("### ").append(command.getName()).append("\n\n");
                    asList(command.getDescription().split("\n"))
                        .forEach(line -> sb.append(line).append("\n\n"));
                    if (!command.getAliases().isEmpty()) {
                        sb.append("**Aliases:** ");
                        sb.append(command.getAliases().stream().collect(Collectors.joining("` / `", "`", "`")));
                        sb.append("\n\n");
                    }
                    if (!command.getUsageLines().isEmpty()) {
                        sb.append("**Usage**").append("\n\n");
                        command.getUsageLines().forEach(line -> {
                            sb.append("  ```").append(command.getName());
                            if (!line.isBlank()) sb.append(" ").append(line);
                            sb.append("```\n\n");
                        });
                        sb.append("\n");
                    }
                });
            sb.append("\n");
        });

        try (Writer writer = new FileWriter(outputFile)) {
            writer.write(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
