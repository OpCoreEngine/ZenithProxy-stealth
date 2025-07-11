package com.zenith;

import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TranslatableTextParserTest {

    @Test
    public void translatableTextComponentParseTest() {
        Logger blah = Globals.CLIENT_LOG; // init in shared static block
        final String chatText = "{\"translate\":\"chat.type.text\",\"with\":[{\"text\":\"bonk2b2t\"},{\"text\":\"you should never talk about that with them\"}]}";
        Component deserialize = ComponentSerializer.deserialize(chatText);
        assertInstanceOf(TranslatableComponent.class, deserialize);
        String serialize = ComponentSerializer.serializePlain(deserialize);
        assertEquals("<bonk2b2t> you should never talk about that with them", serialize);
    }

    @Test
    public void translatableTextRenderingTest() {
        var json = "{\"translate\":\"%s\",\"with\":[{\"extra\":[\"<\",{\"color\":\"dark_red\",\"text\":\"rfresh2\"},\"> hello\"],\"text\":\"\"}]}";
        String expectedText = "<rfresh2> hello";
        var component = ComponentSerializer.deserialize(json);

        String rendered = ComponentSerializer.serializePlain(component);

        assertEquals(expectedText, rendered);
    }
}
