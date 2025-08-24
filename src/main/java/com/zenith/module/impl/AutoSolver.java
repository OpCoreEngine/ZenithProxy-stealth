package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.cache.data.inventory.Container;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.autosolver.AutoSolverOpenScreenHandler;
import com.zenith.feature.autosolver.AutoSolverSignEditorHandler;
import com.zenith.feature.inventory.actions.ClickItem;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.module.api.Module;
import com.zenith.network.codec.PacketHandlerCodec;
import com.zenith.network.codec.PacketHandlerStateCodec;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundOpenSignEditorPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundEditBookPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSoundPacket;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.Sound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.SoundCategory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class AutoSolver extends Module {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("Yukarıya Yazın: ?(\\d+)");
    private static final int LIME_CONCRETE_ID = ItemRegistry.LIME_CONCRETE.id();

    public AutoSolver() {
        super();
    }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleClientTick)
        );
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.client.extra.autoSolver.enabled;
    }

    @Override
    public PacketHandlerCodec registerClientPacketHandlerCodec() {
        return PacketHandlerCodec.clientBuilder()
            .setId("autosolver")
            .setPriority(-10) // after container handlers
            .state(ProtocolState.GAME, PacketHandlerStateCodec.clientBuilder()
                .inbound(ClientboundOpenScreenPacket.class, new AutoSolverOpenScreenHandler())
                .inbound(ClientboundOpenSignEditorPacket.class, new AutoSolverSignEditorHandler())
                .build())
            .build();
    }

    public boolean handleOpenScreen(final ClientboundOpenScreenPacket packet) {
        if (!enabledSetting()) return true;
        
        String titleText = extractTitleText(packet.getTitle());
        info("AutoSolver: Screen opened - " + titleText);
        
        // Handle Kontrol window
        if ("Kontrol".equals(titleText)) {
            // Reset processing flag on new captcha
            isProcessingCaptcha = false;
            
            info("AutoSolver: Detected Kontrol window, searching for lime concrete block");
            
            // Play firework sound 3 times to alert user
            playFireworkSounds();
            
            // Wait longer for container to be fully loaded
            // Schedule delayed execution
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                handleKontrolWindow();
            }, 150, java.util.concurrent.TimeUnit.MILLISECONDS);
        } else {
            // Reset processing flag for non-Kontrol windows
            isProcessingCaptcha = false;
        }
        
        return true;
    }

    public boolean handleOpenSignEditor(final ClientboundOpenSignEditorPacket packet) {
        if (!enabledSetting()) return true;
        
        info("AutoSolver: Sign editor opened, looking for number to write");
        
        // Wait a short delay before trying to read the sign
        // Schedule delayed execution
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            handleSignEditorWindow();
        }, 100, java.util.concurrent.TimeUnit.MILLISECONDS);
        
        return true;
    }

    private void handleClientTick(final ClientBotTick event) {
        // Check for open containers on each tick
        if (!enabledSetting()) return;
        
        // This method will be called every game tick while enabled
        // Container handling is done in packet handler for immediate response
    }

    private volatile boolean isProcessingCaptcha = false;
    
    private void handleKontrolWindow() {
        try {
            // Prevent multiple simultaneous processing
            if (isProcessingCaptcha) {
                info("AutoSolver: Already processing captcha, skipping...");
                return;
            }
            
            isProcessingCaptcha = true;
            
            Container openContainer = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
            if (openContainer == null) {
                warn("AutoSolver: No open container found");
                isProcessingCaptcha = false;
                return;
            }

            info("AutoSolver: Container ID: " + openContainer.getContainerId() + ", Size: " + openContainer.getContents().size());
            
            // Search for lime concrete block and number pattern in the container
            for (int slot = 0; slot < openContainer.getContents().size(); slot++) {
                ItemStack item = openContainer.getContents().get(slot);
                if (item != null && item.getId() == LIME_CONCRETE_ID) {
                    info("AutoSolver: Found lime concrete block at slot " + slot + ", clicking...");
                    
                    // Click the lime concrete block with proper inventory action
                    try {
                        // Use a simple direct click approach
                        info("AutoSolver: Clicking lime concrete block at slot " + slot);
                        
                        // Send click packet directly
                        var clickPacket = new ClickItem(openContainer.getContainerId(), slot, ClickItemAction.LEFT_CLICK);
                        var packet = clickPacket.packet();
                        if (packet != null) {
                            sendClientPacketAsync(packet);
                            info("AutoSolver: Successfully clicked lime concrete block at slot " + slot);
                        } else {
                            warn("AutoSolver: Failed to create click packet");
                        }
                        
                        isProcessingCaptcha = false;
                        return;
                    } catch (Exception e) {
                        error("AutoSolver: Failed to click lime concrete block", e);
                    }
                }
                
                // Also check for sign items with number pattern
                if (item != null && item.getId() == ItemRegistry.OAK_SIGN.id()) {
                    String itemText = extractItemText(item);
                    Matcher matcher = NUMBER_PATTERN.matcher(itemText);
                    
                    if (matcher.find()) {
                        String number = matcher.group(1);
                        info("AutoSolver: Found number in container: " + number);
                        setStoredNumber(number);
                    }
                }
            }
            warn("AutoSolver: Lime concrete block not found in Kontrol window");
            isProcessingCaptcha = false;
        } catch (Exception e) {
            error("AutoSolver: Error handling Kontrol window", e);
            isProcessingCaptcha = false;
        }
    }

    private void handleSignEditorWindow() {
        try {
            // The sign editor doesn't use containers, so we need to check the last opened container
            // for the number pattern that was previously displayed
            info("AutoSolver: Processing sign editor window");
            
            // In a real implementation, we would need to store the number from the previous container
            // For now, let's implement a basic version that writes a test number
            // This would need to be enhanced to properly capture the number from the previous context
            
            String numberToWrite = getStoredNumber();
            if (numberToWrite != null) {
                info("AutoSolver: Writing stored number: " + numberToWrite);
                writeToSign(numberToWrite);
            } else {
                warn("AutoSolver: No stored number found to write to sign");
            }
        } catch (Exception e) {
            error("AutoSolver: Error handling sign editor window", e);
        }
    }
    
    private String storedNumber = null;
    
    private String getStoredNumber() {
        return storedNumber;
    }
    
    private void setStoredNumber(String number) {
        this.storedNumber = number;
        info("AutoSolver: Stored number for sign writing: " + number);
    }

    private void writeToSign(String text) {
        // Create the sign text lines
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(text);
        lines.add("");
        lines.add("");
        lines.add("");

        // Send the edit book packet to update the sign
        ServerboundEditBookPacket packet = new ServerboundEditBookPacket(0, lines, "");
        sendClientPacketAsync(packet);
    }

    private String extractTitleText(Component title) {
        if (title == null) return "";
        
        // Handle plain text
        if (title instanceof net.kyori.adventure.text.TextComponent textComponent) {
            return textComponent.content();
        }
        
        // Fallback to string representation
        return title.toString().replaceAll("\\{[^}]*\\}", "").trim();
    }

    private String extractItemText(ItemStack item) {
        if (item == null || item.getDataComponents() == null) return "";
        
        StringBuilder text = new StringBuilder();
        
        // Try to extract display name and lore from item
        // This is a simplified approach - might need adjustment based on actual data structure
        var components = item.getDataComponents();
        
        // Add any text we can find from the item
        text.append(item.toString());
        
        return text.toString();
    }
    
    private void playFireworkSounds() {
        try {
            // Play 4 firework sounds with slight delays
            for (int i = 0; i < 4; i++) {
                final int delay = i * 200; // 200ms between each sound
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                    try {
                        // Get player position for sound location
                        double x = CACHE.getPlayerCache().getX();
                        double y = CACHE.getPlayerCache().getY();
                        double z = CACHE.getPlayerCache().getZ();
                        
                        // Create firework sound packet
                        ClientboundSoundPacket soundPacket = new ClientboundSoundPacket(
                            BuiltinSound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                            SoundCategory.MASTER,
                            x, y, z,
                            1.0f, // volume
                            1.0f, // pitch  
                            0L    // seed
                        );
                        
                        sendClientPacketAsync(soundPacket);
                        info("AutoSolver: Played firework alert sound");
                    } catch (Exception e) {
                        warn("AutoSolver: Failed to play firework sound: " + e.getMessage());
                    }
                }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            error("AutoSolver: Error scheduling firework sounds", e);
        }
    }
}