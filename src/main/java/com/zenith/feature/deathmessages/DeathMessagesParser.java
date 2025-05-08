package com.zenith.feature.deathmessages;

import com.zenith.discord.Embed;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import static com.zenith.Globals.*;
import static com.zenith.feature.deathmessages.DeathMessageSchemaInstance.spaceSplit;

public class DeathMessagesParser {
    private static final List<DeathMessageSchemaInstance> deathMessageSchemaInstances = new ArrayList<>();
    private static final List<String> mobs = new ArrayList<>();
    static {
        init();
    }

    private static void init() {
        List<String> mobsTemp = Collections.emptyList();
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(DeathMessagesParser.class.getClassLoader().getResourceAsStream("death_message_mobs.schema")))) {
                mobsTemp = br.lines()
                    .filter(l -> !l.isEmpty()) //any empty lines
                    .filter(l -> !l.startsWith("#")) //comments
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing mobs for death message parsing", e);
        }
        mobs.addAll(mobsTemp);
        List<DeathMessageSchemaInstance> schemaInstancesTemp = Collections.emptyList();
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(DeathMessagesParser.class.getClassLoader().getResourceAsStream("death_messages.schema")))) {
                schemaInstancesTemp = br.lines()
                    .filter(l -> !l.isEmpty()) //any empty lines
                    .filter(l -> !l.startsWith("#")) //comments
                    .map(l -> new DeathMessageSchemaInstance(l, mobs))
                    .toList();
            }
        } catch (final Exception e) {
            CLIENT_LOG.error("Error initializing death message schemas", e);
        }
        deathMessageSchemaInstances.addAll(schemaInstancesTemp);
    }

    public Optional<DeathMessageParseResult> parse(final Component component, final String rawInput) {
        List<String> playerNames = getPlayerNames(component);
        List<String> inputSplit = spaceSplit(rawInput);
        for (int i = 0; i < deathMessageSchemaInstances.size(); i++) {
            final DeathMessageSchemaInstance instance = deathMessageSchemaInstances.get(i);
            final Optional<DeathMessageParseResult> parse = instance.parse(inputSplit, playerNames);
            if (parse.isPresent()) return parse;
        }
        if (CONFIG.database.enabled && CONFIG.database.deathsEnabled && CONFIG.database.unknownDeathDiscordMsg) {
            DISCORD.sendEmbedMessage(Embed.builder()
                                         .title("Unknown death message")
                                         .description(ComponentSerializer.serializeJson(component))
                                         .addField("Message", rawInput, false)
                                         .errorColor());
        }
        DEFAULT_LOG.warn("No death message schema found for '{}'", rawInput);
        return Optional.empty();
    }

    List<String> getPlayerNames(final Component component) {
        return component.children().stream()
            .map(Component::clickEvent)
            .filter(Objects::nonNull)
            .filter(clickEvent -> clickEvent.action() == ClickEvent.Action.SUGGEST_COMMAND && clickEvent.value().startsWith("/w"))
            .map(clickEvent -> clickEvent.value().substring(3).trim())
            .toList();
    }
}
