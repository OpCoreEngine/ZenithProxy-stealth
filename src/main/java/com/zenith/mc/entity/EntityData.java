package com.zenith.mc.entity;

import com.zenith.mc.RegistryData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

public record EntityData(
    int id,
    String name,
    float width,
    float height,
    boolean attackable,
    boolean pickable,
    EntityType mcplType
) implements RegistryData { }
