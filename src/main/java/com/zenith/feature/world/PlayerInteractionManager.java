package com.zenith.feature.world;

import com.zenith.Proxy;
import com.zenith.cache.data.inventory.Container;
import com.zenith.feature.world.raycast.BlockRaycastResult;
import com.zenith.feature.world.raycast.EntityRaycastResult;
import com.zenith.mc.block.*;
import com.zenith.mc.enchantment.EnchantmentData;
import com.zenith.mc.item.ItemData;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.ToolTag;
import com.zenith.mc.item.ToolTier;
import com.zenith.module.impl.PlayerSimulation;
import com.zenith.util.math.MathHelper;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.ItemEnchantments;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.*;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.zenith.Shared.BLOCK_DATA;
import static com.zenith.Shared.CACHE;

public class PlayerInteractionManager {
    private int destroyBlockPosX = -1;
    private int destroyBlockPosY = -1;
    private int destroyBlockPosZ = -1;
    private @Nullable ItemStack destroyingItem = Container.EMPTY_STACK;
    private double destroyProgress;
    private double destroyTicks;
    private int destroyDelay;
    private final int destroyDelayInterval = 6;
    private boolean isDestroying;
    private final PlayerSimulation player;
    private int carriedIndex;

    public PlayerInteractionManager(final PlayerSimulation playerSimulation) {
        this.player = playerSimulation;
    }

    private boolean sameDestroyTarget(final int x, final int y, final int z) {
        ItemStack itemStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        return x == this.destroyBlockPosX && y == this.destroyBlockPosY && z == this.destroyBlockPosZ
            && Objects.equals(this.destroyingItem, itemStack);
    }

    public boolean isDestroying() {
        return this.isDestroying;
    }

    public boolean startDestroyBlock(final int x, final int y, final int z, Direction face) {
        if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            player.debug("[{}] [{}, {}, {}] StartDestroyBlock START: Creative break", System.currentTimeMillis(), x, y, z);
            Proxy.getInstance().getClient().sendAsync(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DESTROY_BLOCK,
                    x, y, z,
                    face.mcpl(),
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                )
            );
            destroyBlock(x, y, z);
            this.destroyDelay = destroyDelayInterval;
        } else if (!this.isDestroying || !this.sameDestroyTarget(x, y, z)) {
            if (this.isDestroying) {
                player.debug("[{}] [{}, {}, {}] StartDestroyBlock CANCEL: Changed destroy target", System.currentTimeMillis(), x, y, z);
                Proxy.getInstance().getClient().sendAsync(
                    new ServerboundPlayerActionPacket(
                        PlayerAction.ABORT_DESTROY_BLOCK,
                        this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ,
                        face.mcpl(),
                        0
                    )
                );
            }

            BlockState blockState = World.getBlockState(x, y, z);
            if (BLOCK_DATA.isAir(blockState.block()) || blockBreakSpeed(blockState) < 1.0) {
                this.isDestroying = true;
                this.destroyBlockPosX = x;
                this.destroyBlockPosY = y;
                this.destroyBlockPosZ = z;
                this.destroyingItem = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
                this.destroyProgress = 0.0;
                this.destroyTicks = 0.0F;
                player.debug("[{}] [{}, {}, {}] StartDestroyBlock START: Start multi-tick break", System.currentTimeMillis(), x, y, z);
            } else {
                destroyBlock(x, y, z);
                player.debug("[{}] [{}, {}, {}] StartDestroyBlock START: Instant break", System.currentTimeMillis(), x, y, z);
            }

            Proxy.getInstance().getClient().send(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DESTROY_BLOCK,
                    x, y, z,
                    face.mcpl(),
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()));
        }

        return true;
    }

    public void stopDestroyBlock() {
        if (this.isDestroying) {
            player.debug("[{}] [{}, {}, {}] StopDestroyBlock CANCEL", System.currentTimeMillis(), this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ);
            Proxy.getInstance().getClient()
                .send(new ServerboundPlayerActionPacket(
                    PlayerAction.ABORT_DESTROY_BLOCK,
                    this.destroyBlockPosX, this.destroyBlockPosY, this.destroyBlockPosZ,
                    Direction.DOWN.mcpl(),
                    0
                ));
        }
        this.isDestroying = false;
        this.destroyProgress = 0;
    }

    public boolean continueDestroyBlock(final int x, final int y, final int z, Direction directionFacing) {
        if (this.destroyDelay > 0) {
            --this.destroyDelay;
            return true;
        } else if (CACHE.getPlayerCache().getGameMode() == GameMode.CREATIVE) {
            this.destroyDelay = destroyDelayInterval;
            Proxy.getInstance().getClient().send(
                new ServerboundPlayerActionPacket(
                    PlayerAction.START_DESTROY_BLOCK,
                    x, y, z,
                    directionFacing.mcpl(),
                    CACHE.getPlayerCache().getSeqId().incrementAndGet()
                ));
            destroyBlock(x, y, z);
            player.debug("[{}] [{}, {}, {}] ContinueDestroyBlock START: Creative Break", System.currentTimeMillis(), x, y, z);
            return true;
        } else if (this.sameDestroyTarget(x, y, z)) {
            BlockState blockState = World.getBlockState(x, y, z);
            if (BLOCK_DATA.isAir(blockState.block())) {
                this.isDestroying = false;
                return false;
            } else {
                this.destroyProgress += blockBreakSpeed(blockState);
                ++this.destroyTicks;
                if (this.destroyProgress >= 1.0F) {
                    this.isDestroying = false;
                    Proxy.getInstance().getClient().send(
                        new ServerboundPlayerActionPacket(
                            PlayerAction.STOP_DESTROY_BLOCK,
                            x, y, z,
                            directionFacing.mcpl(),
                            CACHE.getPlayerCache().getSeqId().incrementAndGet()
                        ));
                    destroyBlock(x, y, z);
                    this.destroyProgress = 0.0F;
                    this.destroyTicks = 0.0F;
                    this.destroyDelay = destroyDelayInterval;
                    player.debug("[{}] [{}, {}, {}] ContinueDestroyBlock FINISH", System.currentTimeMillis(), x, y, z);
                }
                return true;
            }
        } else {
            return this.startDestroyBlock(x, y, z, directionFacing);
        }
    }

    public int getDestroyStage() {
        return this.destroyProgress > 0.0 ? (int)(this.destroyProgress * 10.0) : -1;
    }

    public double blockBreakSpeed(BlockState state) {
        double destroySpeed = state.block().destroySpeed();
        double toolFactor = hasCorrectToolForDrops(state, CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND))
            ? 30.0
            : 100.0;
        double playerDestroySpeed = getPlayerDestroySpeed(state);
        return playerDestroySpeed / destroySpeed / toolFactor;
    }

    public double blockBreakSpeed(BlockState state, ItemStack item) {
        double destroySpeed = state.block().destroySpeed();
        double toolFactor = hasCorrectToolForDrops(state, item) ? 30.0 : 100.0;
        double playerDestroySpeed = getPlayerDestroySpeed(state);
        return playerDestroySpeed / destroySpeed / toolFactor;
    }

    public boolean hasCorrectToolForDrops(BlockState state, ItemStack item) {
        if (!state.block().requiresCorrectToolForDrops()) return true;
        if (item == Container.EMPTY_STACK) return false;
        ItemData itemData = ItemRegistry.REGISTRY.get(item.getId());
        if (itemData == null) return false;
        ToolTag toolTag = itemData.toolTag();
        if (toolTag == null) return false;
        var blockTags = state.block().blockTags();
        if (!blockTags.contains(toolTag.type().getBlockTag())) return false;
        if (blockTags.contains(BlockTags.NEEDS_STONE_TOOL)) {
            return toolTag.tier() == ToolTier.STONE || toolTag.tier() == ToolTier.IRON || toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        if (blockTags.contains(BlockTags.NEEDS_IRON_TOOL)) {
            return toolTag.tier() == ToolTier.IRON || toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        if (blockTags.contains(BlockTags.NEEDS_DIAMOND_TOOL)) {
            return toolTag.tier() == ToolTier.DIAMOND || toolTag.tier() == ToolTier.GOLD || toolTag.tier() == ToolTier.NETHERITE;
        }
        return true;
    }

    public boolean matchingTool(ItemStack item, Block block) {
        ItemData itemData = ItemRegistry.REGISTRY.get(item.getId());
        if (itemData == null) return false;
        ToolTag toolTag = itemData.toolTag();
        if (toolTag == null) return false;
        return block.blockTags().contains(toolTag.type().getBlockTag());
    }

    public int getEnchantmentLevel(ItemStack item, EnchantmentData enchantmentData) {
        if (item == Container.EMPTY_STACK) return 0;
        DataComponents dataComponents = item.getDataComponents();
        if (dataComponents == null) return 0;
        ItemEnchantments itemEnchantments = dataComponents.get(DataComponentType.ENCHANTMENTS);
        if (itemEnchantments == null) return 0;
        if (!itemEnchantments.getEnchantments().containsKey(enchantmentData.id())) return 0;
        return itemEnchantments.getEnchantments().get(enchantmentData.id());
    }

    public double getPlayerDestroySpeed(BlockState state) {
        double speed = 1.0;
        var mainHandStack = CACHE.getPlayerCache().getEquipment(EquipmentSlot.MAIN_HAND);
        if (mainHandStack != Container.EMPTY_STACK) {
            if (matchingTool(mainHandStack, state.block())) {
                ItemData itemData = ItemRegistry.REGISTRY.get(mainHandStack.getId());
                ToolTag toolTag = itemData.toolTag();
                speed = toolTag.tier().getSpeed();
            }
        }

        if (speed > 1.0) {
            float miningEfficiencyAttribute = player
                .getAttributeValue(AttributeType.Builtin.PLAYER_MINING_EFFICIENCY, 0);
            speed += miningEfficiencyAttribute;
        }

        boolean hasDigSpeedEffect = false;
        int hasteAmplifier = 0;
        var hasteEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.HASTE);
        if (hasteEffect != null) {
            hasDigSpeedEffect = true;
            hasteAmplifier = hasteEffect.getAmplifier();
        }
        int conduitPowerAmplifier = 0;
        var conduitPowerEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.CONDUIT_POWER);
        if (conduitPowerEffect != null) {
            hasDigSpeedEffect = true;
            conduitPowerAmplifier = conduitPowerEffect.getAmplifier();
        }

        if (hasDigSpeedEffect) {
            int digSpeedAmplification = Math.max(hasteAmplifier, conduitPowerAmplifier);
            speed *= 1.0 + (digSpeedAmplification + 1) * 0.2;
        }

        var miningFatigueEffect = CACHE.getPlayerCache().getThePlayer().getPotionEffectMap().get(Effect.MINING_FATIGUE);

        if (miningFatigueEffect != null) {
            speed *= switch(miningFatigueEffect.getAmplifier()) {
                case 0 -> 0.3;
                case 1 -> 0.09;
                case 2 -> 0.0027;
                default -> 8.1E-4;
            };
        }

        speed *= player.getAttributeValue(AttributeType.Builtin.PLAYER_BLOCK_BREAK_SPEED, 1.0f);

        boolean isEyeInWater = World.isWater(
            World.getBlock(
                MathHelper.floorI(player.getX()),
                MathHelper.floorI(player.getEyeY()),
                MathHelper.floorI(player.getZ())));
        if (isEyeInWater) {
            speed *= player.getAttributeValue(AttributeType.Builtin.PLAYER_SUBMERGED_MINING_SPEED, 0.2f);
        }

        if (!player.isOnGround()) {
            speed /= 5.0;
        }

        return speed;
    }

    private void destroyBlock(int x, int y, int z) {
        CACHE.getChunkCache().getChunkSection(x, y, z)
            .setBlock(x & 15, y & 15, z & 15, BlockRegistry.AIR.id());
    }

    public InteractionResult interact(Hand hand, EntityRaycastResult ray) {
        Proxy.getInstance().getClient().send(new ServerboundInteractPacket(
            ray.entity().getEntityId(),
            InteractAction.INTERACT,
            0, 0, 0,
            hand,
            player.isSneaking()
        ));
        return InteractionResult.PASS;
    }

    public InteractionResult interactAt(Hand hand, EntityRaycastResult ray) {
        Proxy.getInstance().getClient().send(new ServerboundInteractPacket(
            ray.entity().getEntityId(),
            InteractAction.INTERACT_AT,
            0, 0, 0,
            hand,
            player.isSneaking()
        ));
        return InteractionResult.PASS;
    }

    public InteractionResult useItemOn(Hand hand, BlockRaycastResult ray) {
        Proxy.getInstance().getClient().send(new ServerboundUseItemOnPacket(
            ray.x(), ray.y(), ray.z(),
            ray.direction().mcpl(),
            hand,
            // todo: cursor raytrace
            0, 0, 0,
            false,
            CACHE.getPlayerCache().getSeqId().incrementAndGet()
        ));
        // todo: check if we are placing a block
        //  if so, add the block to the world so we don't have a brief desync
        return InteractionResult.PASS;
    }

    public InteractionResult useItem(Hand hand) {
        Proxy.getInstance().getClient().send(new ServerboundUseItemPacket(
            hand,
            CACHE.getPlayerCache().getSeqId().incrementAndGet(),
            player.getYaw(),
            player.getPitch()
        ));
        return InteractionResult.PASS;
    }

    public void ensureHasSentCarriedItem() {
        int heldItemSlot = CACHE.getPlayerCache().getHeldItemSlot();
        if (carriedIndex != heldItemSlot) {
            player.debug("[{}] Syncing held item slot: {} -> {}", System.currentTimeMillis(), heldItemSlot, carriedIndex);
            Proxy.getInstance().getClient().sendAwait(new ServerboundSetCarriedItemPacket(carriedIndex));
        }
    }

    public void attackEntity(final EntityRaycastResult entity) {
        player.debug("[{}] [{}, {}, {}] Attack Entity", System.currentTimeMillis(), entity.entity().getX(), entity.entity().getY(), entity.entity().getZ());
        Proxy.getInstance().getClient().sendAsync(new ServerboundInteractPacket(entity.entity().getEntityId(), InteractAction.ATTACK, false));
        Proxy.getInstance().getClient().sendAsync(new ServerboundSwingPacket(Hand.MAIN_HAND));
    }
}
