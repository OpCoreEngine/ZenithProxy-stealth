package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.cache.data.entity.EntityLiving;
import com.zenith.cache.data.entity.EntityPlayer;
import com.zenith.event.module.ServerPlayerInVisualRangeEvent;
import com.zenith.module.api.Module;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.EntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.MetadataTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.BooleanEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ByteEntityMetadata;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.type.ObjectEntityMetadata;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetEntityDataPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class GlowingEffect extends Module {

    private static final int ENTITY_STATUS_METADATA_INDEX = 0;
    private static final int CUSTOM_NAME_METADATA_INDEX = 2;
    private static final int CUSTOM_NAME_VISIBLE_METADATA_INDEX = 3;
    
    // Debug: Try alternative metadata indices in case they're wrong
    private static final int ALT_CUSTOM_NAME_INDEX = 3;
    private static final int ALT_NAME_VISIBLE_INDEX = 4;
    private static final byte GLOWING_FLAG = 0x40;

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ServerPlayerInVisualRangeEvent.class, this::handlePlayerEnterVisualRange)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.glowingEffect.enabled;
    }
    
    @Override
    public void onEnable() {
        super.onEnable();
        // Force update all existing players when module is enabled
        forceUpdateAllPlayers();
    }

    public void handlePlayerEnterVisualRange(ServerPlayerInVisualRangeEvent event) {
        if (!this.isEnabled()) return;
        
        // Send glowing effect immediately when player enters visual range
        sendGlowingEffectToPlayer(event.playerEntity());
    }
    
    // Force update all existing players when module is enabled
    public void forceUpdateAllPlayers() {
        if (!this.isEnabled()) return;
        
        // Update all existing players in cache
        CACHE.getEntityCache().getEntities().values().forEach(entity -> {
            if (entity instanceof EntityPlayer player && !player.equals(CACHE.getPlayerCache().getThePlayer())) {
                sendGlowingEffectToPlayer(player);
            }
        });
    }

    private void sendGlowingEffectToPlayer(EntityPlayer player) {
        if (player.equals(CACHE.getPlayerCache().getThePlayer())) return;
        
        // Create entity data packet with glowing effect and enhanced nametag
        List<EntityMetadata<?, ?>> metadata = new ArrayList<>();
        
        // 1. Add glowing effect only (no fire - too distracting)
        metadata.add(new ByteEntityMetadata(ENTITY_STATUS_METADATA_INDEX, MetadataTypes.BYTE, GLOWING_FLAG));
        
        // 2. ALWAYS add enhanced custom name for players
        Component enhancedName = createEnhancedName(player);
        metadata.add(new ObjectEntityMetadata<>(CUSTOM_NAME_METADATA_INDEX, MetadataTypes.OPTIONAL_COMPONENT, 
            Optional.of(enhancedName)));
        
        // 3. ALWAYS force nametag visible
        metadata.add(new BooleanEntityMetadata(CUSTOM_NAME_VISIBLE_METADATA_INDEX, MetadataTypes.BOOLEAN, true));
        
        ClientboundSetEntityDataPacket glowPacket = new ClientboundSetEntityDataPacket(player.getEntityId(), metadata);
        
        // Send to active player
        if (Proxy.getInstance().hasActivePlayer()) {
            Proxy.getInstance().getActivePlayer().sendAsync(glowPacket);
        }
        
        // Update entity's metadata cache
        for (EntityMetadata<?, ?> meta : metadata) {
            player.getMetadata().put(meta.getId(), meta);
        }
    }

    public void interceptEntityDataPacket(ClientboundSetEntityDataPacket packet) {
        if (!this.isEnabled()) return;
        
        
        var entity = CACHE.getEntityCache().get(packet.getEntityId());
        if (entity == null || !(entity instanceof EntityLiving)) return;
        
        // Skip self player
        if (entity.equals(CACHE.getPlayerCache().getThePlayer())) return;
        
        // Filter out unwanted entities
        String entityType = entity.getEntityType().name();
        if (entityType.equals("ARMOR_STAND") || 
            entityType.equals("TEXT_DISPLAY") || 
            entityType.equals("ITEM_DISPLAY") ||
            entityType.equals("BLOCK_DISPLAY")) return;
        
        boolean isPlayer = entity instanceof EntityPlayer;
        boolean isMob = !isPlayer;
        
        // Check if we should process this entity type
        if (isPlayer && !CONFIG.client.extra.glowingEffect.enhancedNameTags) return;
        if (isMob && !CONFIG.client.extra.glowingEffect.showMobs) return;
        
        // SAFE approach: Preserve original metadata, only modify what we need
        List<EntityMetadata<?, ?>> modifiedMetadata = new ArrayList<>(packet.getMetadata());
        
        // Remove only our target metadata to replace them
        modifiedMetadata.removeIf(meta -> 
            meta.getId() == ENTITY_STATUS_METADATA_INDEX ||
            meta.getId() == CUSTOM_NAME_METADATA_INDEX ||
            meta.getId() == CUSTOM_NAME_VISIBLE_METADATA_INDEX
        );
        
        // Add glowing effect
        modifiedMetadata.add(new ByteEntityMetadata(ENTITY_STATUS_METADATA_INDEX, MetadataTypes.BYTE, GLOWING_FLAG));
        
        // Create and add enhanced name
        Component enhancedName = null;
        if (isPlayer) {
            enhancedName = createEnhancedName((EntityPlayer) entity);
        } else {
            enhancedName = createEnhancedMobName((EntityLiving) entity);
        }
        
        if (enhancedName != null && enhancedName != Component.empty()) {
            // Add ONLY the standard metadata indices (safe approach)
            modifiedMetadata.add(new ObjectEntityMetadata<>(CUSTOM_NAME_METADATA_INDEX, MetadataTypes.OPTIONAL_COMPONENT, 
                Optional.of(enhancedName)));
            modifiedMetadata.add(new BooleanEntityMetadata(CUSTOM_NAME_VISIBLE_METADATA_INDEX, MetadataTypes.BOOLEAN, true));
        }
        
        // Skip aggressive reapplication to avoid protocol issues
        
        // Replace packet metadata with proper ordering
        packet.getMetadata().clear();
        
        // Add metadata in proper order (status first, then name, then visibility)
        for (EntityMetadata<?, ?> meta : modifiedMetadata) {
            if (meta.getId() == ENTITY_STATUS_METADATA_INDEX) {
                packet.getMetadata().add(meta);
                break;
            }
        }
        for (EntityMetadata<?, ?> meta : modifiedMetadata) {
            if (meta.getId() == CUSTOM_NAME_METADATA_INDEX) {
                packet.getMetadata().add(meta);
                break;
            }
        }
        for (EntityMetadata<?, ?> meta : modifiedMetadata) {
            if (meta.getId() == CUSTOM_NAME_VISIBLE_METADATA_INDEX) {
                packet.getMetadata().add(meta);
                break;
            }
        }
        // Add remaining metadata
        for (EntityMetadata<?, ?> meta : modifiedMetadata) {
            if (meta.getId() != ENTITY_STATUS_METADATA_INDEX && 
                meta.getId() != CUSTOM_NAME_METADATA_INDEX && 
                meta.getId() != CUSTOM_NAME_VISIBLE_METADATA_INDEX) {
                packet.getMetadata().add(meta);
            }
        }
        
        // Simplified approach - just modify the packet metadata
    }
    
    // Schedule a reapplication of enhancement to prevent server overrides
    private void scheduleEnhancementReapply(int entityId, boolean isPlayer, EntityLiving entity) {
        // Send enhancement packet after a short delay
        new Thread(() -> {
            try {
                Thread.sleep(500); // 500ms delay to ensure server packets finish
                if (Proxy.getInstance().hasActivePlayer()) {
                    List<EntityMetadata<?, ?>> reinforcementMetadata = new ArrayList<>();
                    
                    // Reapply glowing effect only
                    reinforcementMetadata.add(new ByteEntityMetadata(ENTITY_STATUS_METADATA_INDEX, MetadataTypes.BYTE, GLOWING_FLAG));
                    
                    // Reapply enhanced name
                    Component enhancedName;
                    if (isPlayer) {
                        enhancedName = createEnhancedName((EntityPlayer) entity);
                    } else {
                        enhancedName = createEnhancedMobName(entity);
                    }
                    
                    if (enhancedName != null && enhancedName != Component.empty()) {
                        reinforcementMetadata.add(new ObjectEntityMetadata<>(CUSTOM_NAME_METADATA_INDEX, MetadataTypes.OPTIONAL_COMPONENT, 
                            Optional.of(enhancedName)));
                        reinforcementMetadata.add(new BooleanEntityMetadata(CUSTOM_NAME_VISIBLE_METADATA_INDEX, MetadataTypes.BOOLEAN, true));
                    }
                    
                    ClientboundSetEntityDataPacket reinforcementPacket = new ClientboundSetEntityDataPacket(entityId, reinforcementMetadata);
                    Proxy.getInstance().getActivePlayer().sendAsync(reinforcementPacket);
                }
            } catch (Exception e) {
                // Ignore errors in reinforcement
            }
        }).start();
    }
    
    // Send direct enhancement packet immediately
    private void sendDirectEnhancementPacket(int entityId, EntityPlayer player) {
        try {
            List<EntityMetadata<?, ?>> directMetadata = new ArrayList<>();
            
            // Force enhanced name with simple format
            String playerName = "Unknown";
            if (player.getUuid() != null) {
                var tabEntry = CACHE.getTabListCache().get(player.getUuid());
                if (tabEntry.isPresent()) {
                    playerName = tabEntry.get().getName();
                }
            }
            String simpleName = "[[[[ " + playerName.toUpperCase() + " ]]]]";
            Component simpleComponent = Component.text(simpleName).color(NamedTextColor.YELLOW);
            
            directMetadata.add(new ByteEntityMetadata(ENTITY_STATUS_METADATA_INDEX, MetadataTypes.BYTE, GLOWING_FLAG));
            directMetadata.add(new ObjectEntityMetadata<>(CUSTOM_NAME_METADATA_INDEX, MetadataTypes.OPTIONAL_COMPONENT, 
                Optional.of(simpleComponent)));
            directMetadata.add(new BooleanEntityMetadata(CUSTOM_NAME_VISIBLE_METADATA_INDEX, MetadataTypes.BOOLEAN, true));
            
            ClientboundSetEntityDataPacket directPacket = new ClientboundSetEntityDataPacket(entityId, directMetadata);
            Proxy.getInstance().getActivePlayer().sendAsync(directPacket);
            
            System.out.println("GlowingEffect: Sent DIRECT enhancement packet for entity " + entityId + " with name: " + simpleName);
        } catch (Exception e) {
            System.out.println("GlowingEffect: Failed to send direct packet: " + e.getMessage());
        }
    }
    
    // Schedule multiple aggressive reapplications
    private void scheduleMultipleReapplications(int entityId, EntityPlayer player) {
        // Send enhancement packets every 1, 2, 3 seconds to be extra aggressive
        for (int delay : new int[]{1000, 2000, 3000}) {
            new Thread(() -> {
                try {
                    Thread.sleep(delay);
                    if (Proxy.getInstance().hasActivePlayer()) {
                        sendDirectEnhancementPacket(entityId, player);
                        System.out.println("GlowingEffect: Sent AGGRESSIVE reapplication for entity " + entityId);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }).start();
        }
    }
    
    private Component createEnhancedName(EntityPlayer player) {
        String playerName = "Unknown";
        if (player.getUuid() != null) {
            var tabEntry = CACHE.getTabListCache().get(player.getUuid());
            if (tabEntry.isPresent()) {
                playerName = tabEntry.get().getName();
            }
        }
        
        // Try pure ASCII with maximum visibility
        String ultraSimpleName = "[[[[ " + playerName.toUpperCase() + " ]]]]";
        
        // Create the simplest possible component
        Component nameComponent = Component.text(ultraSimpleName)
            .color(NamedTextColor.YELLOW);
        
        // Name created: ultraSimpleName
        
        // Add player health
        if (player instanceof EntityPlayer && player.getHealth() != null) {
            Float currentHealth = player.getHealth();
            String healthText = String.format("%.0f ♥", currentHealth);
            nameComponent = nameComponent
                .append(Component.newline())
                .append(Component.text(healthText)
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD));
        }
        
        return nameComponent;
    }
    
    private Component createEnhancedMobName(EntityLiving mob) {
        // Check for custom name first
        String customName = getCustomMobName(mob);
        String displayName;
        boolean hasCustomName = customName != null && !customName.isEmpty();
        
        if (hasCustomName) {
            displayName = customName;
        } else {
            displayName = getMobDisplayName(mob);
        }
        
        // Get health info - only use actual health data
        Float currentHealth = mob.getHealth();
        
        // Skip if no actual health data to prevent wrong displays
        if (currentHealth == null) {
            // Just show name without health for entities without health data
            NamedTextColor mobColor = hasCustomName ? NamedTextColor.GOLD : getMobSpecificColor(mob);
            return Component.text(displayName)
                .color(mobColor)
                .decorate(TextDecoration.BOLD);
        }
        
        Float maxHealth = getMaxHealth(mob);
        if (maxHealth == null) maxHealth = 20.0f; // Default fallback
        
        // Create name tag
        NamedTextColor mobColor = hasCustomName ? NamedTextColor.GOLD : getMobSpecificColor(mob);
        NamedTextColor healthColor = getHealthColor(currentHealth, maxHealth);
        
        // Display name
        Component nameComponent = Component.text(displayName)
            .color(mobColor)
            .decorate(TextDecoration.BOLD);
        
        // Show health info only if enabled and we have valid data
        if (CONFIG.client.extra.glowingEffect.showMobHealth) {
            String healthText = String.format("%.0f♥", currentHealth);
            nameComponent = nameComponent
                .append(Component.newline())
                .append(Component.text(healthText)
                    .color(healthColor)
                    .decorate(TextDecoration.BOLD));
        }
        
        return nameComponent;
    }
    
    private String getMobDisplayName(EntityLiving mob) {
        String entityTypeName = mob.getEntityType().name();
        // Convert ENTITY_TYPE to Entity Type
        String[] words = entityTypeName.toLowerCase().replace("_", " ").split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
                result.append(" ");
            }
        }
        return result.toString().trim();
    }
    
    private Float getMaxHealth(EntityLiving mob) {
        // Try to get max health from attributes, fallback to common values
        var maxHealthAttr = mob.getAttributes().get(org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType.Builtin.MAX_HEALTH);
        if (maxHealthAttr != null) {
            return (float) maxHealthAttr.getValue();
        }
        
        // Fallback to common mob health values
        String mobType = mob.getEntityType().name();
        return switch (mobType) {
            case "ZOMBIE", "SKELETON", "SPIDER" -> 20.0f;
            case "CREEPER" -> 20.0f;
            case "ENDERMAN" -> 40.0f;
            case "WITCH" -> 26.0f;
            case "WITHER" -> 300.0f;
            case "ENDER_DRAGON" -> 200.0f;
            case "IRON_GOLEM" -> 100.0f;
            case "COW", "PIG", "SHEEP" -> 10.0f;
            case "CHICKEN" -> 4.0f;
            case "WOLF" -> 8.0f;
            default -> 20.0f;
        };
    }
    
    private NamedTextColor getMobSpecificColor(EntityLiving mob) {
        String mobType = mob.getEntityType().name();
        return switch (mobType) {
            // Hostile mobs - Red colors
            case "ZOMBIE", "ZOMBIE_VILLAGER", "HUSK", "DROWNED" -> NamedTextColor.RED;
            case "SKELETON", "STRAY", "WITHER_SKELETON" -> NamedTextColor.WHITE;
            case "CREEPER" -> NamedTextColor.GREEN;
            case "SPIDER", "CAVE_SPIDER" -> NamedTextColor.DARK_RED;
            case "ENDERMAN" -> NamedTextColor.DARK_PURPLE;
            case "WITCH" -> NamedTextColor.DARK_GREEN;
            case "WITHER" -> NamedTextColor.DARK_GRAY;
            case "ENDER_DRAGON" -> NamedTextColor.LIGHT_PURPLE;
            case "BLAZE" -> NamedTextColor.GOLD;
            case "GHAST" -> NamedTextColor.WHITE;
            
            // Neutral/Passive mobs - Different colors
            case "COW" -> NamedTextColor.AQUA;
            case "PIG" -> NamedTextColor.LIGHT_PURPLE;
            case "SHEEP" -> NamedTextColor.WHITE;
            case "CHICKEN" -> NamedTextColor.YELLOW;
            case "WOLF" -> NamedTextColor.GRAY;
            case "CAT" -> NamedTextColor.GOLD;
            case "HORSE", "DONKEY", "MULE" -> NamedTextColor.DARK_AQUA;
            case "VILLAGER" -> NamedTextColor.GREEN;
            case "IRON_GOLEM" -> NamedTextColor.DARK_GRAY;
            
            // Default fallback
            default -> parseColor(CONFIG.client.extra.glowingEffect.mobNameTagColor);
        };
    }
    
    private NamedTextColor getHealthColor(Float currentHealth, Float maxHealth) {
        if (currentHealth == null || maxHealth == null) return NamedTextColor.GREEN;
        
        float healthPercent = currentHealth / maxHealth;
        
        if (healthPercent > 0.75f) return NamedTextColor.GREEN;      // 75%+ = Green
        else if (healthPercent > 0.50f) return NamedTextColor.YELLOW; // 50-75% = Yellow
        else if (healthPercent > 0.25f) return NamedTextColor.GOLD;   // 25-50% = Orange
        else return NamedTextColor.RED;                               // 0-25% = Red
    }
    
    private String getCustomMobName(EntityLiving mob) {
        try {
            // Check if the mob has custom name metadata
            var metadata = mob.getMetadata().get(CUSTOM_NAME_METADATA_INDEX);
            if (metadata != null && metadata.getValue() instanceof Optional<?> optional) {
                if (optional.isPresent() && optional.get() instanceof Component nameComponent) {
                    return extractTextFromComponent(nameComponent);
                }
            }
        } catch (Exception e) {
            // Ignore errors and fall back to default name
        }
        return null;
    }
    
    private String extractTextFromComponent(Component component) {
        if (component == null) return null;
        
        // Try to extract plain text from the component
        StringBuilder text = new StringBuilder();
        
        // Handle text components
        if (component instanceof net.kyori.adventure.text.TextComponent textComp) {
            text.append(textComp.content());
        }
        
        // Handle children components
        for (Component child : component.children()) {
            String childText = extractTextFromComponent(child);
            if (childText != null) {
                text.append(childText);
            }
        }
        
        String result = text.toString().trim();
        return result.isEmpty() ? null : result;
    }
    
    private NamedTextColor parseColor(String colorName) {
        try {
            return NamedTextColor.NAMES.value(colorName.toLowerCase());
        } catch (Exception e) {
            return NamedTextColor.YELLOW; // Default fallback
        }
    }
}