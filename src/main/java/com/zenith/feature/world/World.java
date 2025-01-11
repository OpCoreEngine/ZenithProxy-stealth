package com.zenith.feature.world;

import com.zenith.mc.block.*;
import com.zenith.mc.dimension.DimensionData;
import com.zenith.mc.dimension.DimensionRegistry;
import com.zenith.util.math.MathHelper;
import com.zenith.util.math.MutableVec3d;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import lombok.experimental.UtilityClass;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zenith.Shared.*;

@UtilityClass
public class World {
    @Nullable
    public ChunkSection getChunkSection(final int x, final int y, final int z) {
        try {
            return CACHE.getChunkCache().getChunkSection(x, y, z );
        } catch (final Exception e) {
            CLIENT_LOG.error("error finding chunk section for pos: {}, {}, {}", x, y, z, e);
        }
        return null;
    }

    public @NotNull DimensionData dimensionTypeWithFallback() {
        DimensionData currentDimension = CACHE.getChunkCache().getCurrentDimension();
        if (currentDimension == null) return DimensionRegistry.OVERWORLD;
        return currentDimension;
    }

    public boolean isChunkLoadedBlockPos(final int blockX, final int blockZ) {
        return CACHE.getChunkCache().isChunkLoaded(blockX >> 4, blockZ >> 4);
    }

    public boolean isChunkLoadedChunkPos(final int chunkX, final int chunkZ) {
        return CACHE.getChunkCache().isChunkLoaded(chunkX, chunkZ);
    }

    public int getBlockStateId(final BlockPos blockPos) {
        return getBlockStateId(blockPos.x(), blockPos.y(), blockPos.z());
    }

    public int getBlockStateId(final int x, final int y, final int z) {
        final ChunkSection chunk = getChunkSection(x, y, z);
        if (chunk == null) return 0;
        return chunk.getBlock(x & 15, y & 15, z & 15);
    }

    public BlockState getBlockState(final BlockPos blockPos) {
        return getBlockState(blockPos.x(), blockPos.y(), blockPos.z());
    }

    public BlockState getBlockState(final long blockPos) {
        return getBlockState(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos));
    }

    public BlockState getBlockState(final int x, final int y, final int z) {
        return new BlockState(getBlockAtBlockPos(x, y, z), getBlockStateId(x, y, z), x, y, z);
    }

    public Block getBlockAtBlockPos(final BlockPos blockPos) {
        return getBlockAtBlockPos(blockPos.x(), blockPos.y(), blockPos.z());
    }

    public Block getBlockAtBlockPos(final long blockPos) {
        return getBlockAtBlockPos(BlockPos.getX(blockPos), BlockPos.getY(blockPos), BlockPos.getZ(blockPos));
    }

    public Block getBlockAtBlockPos(final int x, final int y, final int z) {
        Block blockData = BLOCK_DATA.getBlockDataFromBlockStateId(getBlockStateId(x, y, z));
        if (blockData == null)
            return BlockRegistry.AIR;
        return blockData;
    }

    public List<LocalizedCollisionBox> getIntersectingCollisionBoxes(final LocalizedCollisionBox cb) {
        final List<LocalizedCollisionBox> boundingBoxList = new ArrayList<>();
        getSolidBlockCollisionBoxes(cb, boundingBoxList);
        getEntityCollisionBoxes(cb, boundingBoxList);
        return boundingBoxList;
    }

    public void getSolidBlockCollisionBoxes(final LocalizedCollisionBox cb, final List<LocalizedCollisionBox> results) {
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            final BlockState blockState = getBlockState(blockPos);
            if (blockState.isSolidBlock()) {
                var collisionBoxes = blockState.getLocalizedCollisionBoxes();
                results.addAll(collisionBoxes);
            }
        }
    }

    public void getEntityCollisionBoxes(final LocalizedCollisionBox cb, final List<LocalizedCollisionBox> results) {
        for (var entity : CACHE.getEntityCache().getEntities().values()) {
            EntityType entityType = entity.getEntityType();
            if (!(entityType == EntityType.BOAT || entityType == EntityType.SHULKER))
                continue;
            if (entity.getPassengerIds().contains(CACHE.getPlayerCache().getThePlayer().getEntityId()))
                continue;
            var entityData = ENTITY_DATA.getEntityData(entityType);
            if (entityData == null) continue;
            var x = entity.getX();
            var y = entity.getY();
            var z = entity.getZ();
            double halfW = entityData.width() / 2.0;
            double minX = x - halfW;
            double minY = y;
            double minZ = z - halfW;
            double maxX = x + halfW;
            double maxY = y + entityData.height();
            double maxZ = z + halfW;
            if (cb.intersects(minX, maxX, minY, maxY, minZ, maxZ)) {
                results.add(new LocalizedCollisionBox(minX, maxX, minY, maxY, minZ, maxZ, x, y, z));
            }
        }
    }

    public boolean isWater(Block block) {
        return block == BlockRegistry.WATER
            || block == BlockRegistry.BUBBLE_COLUMN;
    }

    public boolean isFluid(Block block) {
        return isWater(block) || block == BlockRegistry.LAVA;
    }

    public LongList getBlockPosLongListInCollisionBox(final LocalizedCollisionBox cb) {
        int minX = MathHelper.floorI(cb.minX()) - 1;
        int maxX = MathHelper.ceilI(cb.maxX()) + 1;
        int minY = MathHelper.floorI(cb.minY()) - 1;
        int maxY = MathHelper.ceilI(cb.maxY()) + 1;
        int minZ = MathHelper.floorI(cb.minZ()) - 1;
        int maxZ = MathHelper.ceilI(cb.maxZ()) + 1;
        final LongArrayList blockPosList = new LongArrayList((maxX - minX) * (maxY - minY) * (maxZ - minZ));
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    blockPosList.add(BlockPos.asLong(x, y, z));
                }
            }
        }
        return blockPosList;
    }

    public LongList getBlockPosLongListInCollisionBoxInside(final LocalizedCollisionBox cb) {
        int minX = MathHelper.floorI(cb.minX());
        int maxX = MathHelper.ceilI(cb.maxX());
        int minY = MathHelper.floorI(cb.minY());
        int maxY = MathHelper.ceilI(cb.maxY());
        int minZ = MathHelper.floorI(cb.minZ());
        int maxZ = MathHelper.ceilI(cb.maxZ());
        final LongArrayList blockPosList = new LongArrayList((maxX - minX) * (maxY - minY) * (maxZ - minZ));
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    blockPosList.add(BlockPos.asLong(x, y, z));
                }
            }
        }
        return blockPosList;
    }

    public List<BlockState> getCollidingBlockStates(final LocalizedCollisionBox cb) {
        final List<BlockState> blockStates = new ArrayList<>(4);
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            var blockState = getBlockState(blockPos);
            if (BLOCK_DATA.isAir(blockState.block())) continue; // air
            List<LocalizedCollisionBox> blockStateCBs = blockState.getLocalizedCollisionBoxes();
            for (int j = 0; j < blockStateCBs.size(); j++) {
                if (blockStateCBs.get(j).intersects(cb)) {
                    blockStates.add(blockState);
                    break;
                }
            }
        }
        return blockStates;
    }

    public List<BlockState> getCollidingBlockStatesInside(final LocalizedCollisionBox cb) {
        final List<BlockState> blockStates = new ArrayList<>(4);
        LongList blockPosList = getBlockPosLongListInCollisionBoxInside(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            var blockState = getBlockState(blockPos);
            if (BLOCK_DATA.isAir(blockState.block())) continue; // air
            blockStates.add(blockState);
        }
        return blockStates;
    }

    public static boolean isSpaceEmpty(final LocalizedCollisionBox cb) {
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos = blockPosList.getLong(i);
            var blockStateCBs = getBlockState(blockPos).getLocalizedCollisionBoxes();
            for (int j = 0; j < blockStateCBs.size(); j++) {
                if (blockStateCBs.get(j).intersects(cb)) return false;
            }
        }
        return true;
    }

    public static Optional<BlockPos> findSupportingBlockPos(final LocalizedCollisionBox cb) {
        BlockPos supportingBlock = null;
        double dist = Double.MAX_VALUE;
        LongList blockPosList = getBlockPosLongListInCollisionBox(cb);
        for (int i = 0; i < blockPosList.size(); i++) {
            var blockPos2 = blockPosList.getLong(i);
            var blockState = getBlockState(blockPos2);
            var x = blockState.x();
            var y = blockState.y();
            var z = blockState.z();
            var blockStateCBs = getBlockState(blockPos2).getLocalizedCollisionBoxes();
            for (int j = 0; j < blockStateCBs.size(); j++) {
                if (blockStateCBs.get(j).intersects(cb)) {
                    final double curDist = MathHelper.distanceSq3d(x, y, z, cb.x(), cb.y(), cb.z());
                    if (curDist < dist || curDist == dist && (supportingBlock == null || BlockPos.compare(supportingBlock.x(), supportingBlock.y(), supportingBlock.z(), x, y, z) < 0)) {
                        supportingBlock = new BlockPos(x, y, z);
                        dist = curDist;
                    }
                    break;
                }
            }
        }
        return Optional.ofNullable(supportingBlock);
    }

    public static float getFluidHeight(BlockState localBlockState) {
        var block = localBlockState.block();
        if (block == BlockRegistry.WATER) {
            if (World.getBlockAtBlockPos(localBlockState.x(), localBlockState.y() + 1, localBlockState.z()) == BlockRegistry.WATER) {
                return 1;
            }
            int level = localBlockState.id() - localBlockState.block().minStateId();
            if ((level & 0x8) == 8) return 8 / 9f;
            return (8 - level) / 9f;
        } else if (block == BlockRegistry.LAVA) {
            if (World.getBlockAtBlockPos(localBlockState.x(),
                                         localBlockState.y() + 1,
                                         localBlockState.z()) == BlockRegistry.LAVA) {
                return 1;
            }
            int level = localBlockState.id() - localBlockState.block().minStateId();
            if (level >= 8) return 8 / 9f;
            return (8 - level) / 9f;
        } else if (BLOCK_DATA.isWaterLogged(localBlockState.id())) {
            return 8 / 9f;
        } else {
            return 0f;
        }
    }

    public static MutableVec3d getFluidFlow(BlockState localBlockState) {
        float fluidHeight = Math.min(getFluidHeight(localBlockState), 8 / 9f);
        if (fluidHeight == 0) return new MutableVec3d(0, 0, 0);
        double flowX = 0;
        double flowZ = 0;
        for (var dir : Direction.HORIZONTALS) {
            int x = localBlockState.x() + dir.x();
            int y = localBlockState.y();
            int z = localBlockState.z() + dir.z();
            if (affectsFlow(localBlockState, x, y, z)) {
                float fluidHDiffMult = 0.0F;
                var offsetState = getBlockState(x, y, z);
                float offsetFluidHeight = Math.min(getFluidHeight(offsetState), 8 / 9f);
                if (offsetFluidHeight == 0) {
                    if (affectsFlow(localBlockState, x, y - 1, z)) {
                        offsetFluidHeight = Math.min(getFluidHeight(getBlockState(x, y - 1, z)), 8 / 9f);
                        if (offsetFluidHeight > 0) {
                            fluidHDiffMult = fluidHeight - (offsetFluidHeight - 0.8888889F);
                        }
                    }
                } else if (offsetFluidHeight > 0) {
                    fluidHDiffMult = fluidHeight - offsetFluidHeight;
                }

                if (fluidHDiffMult != 0) {
                    flowX += (float) dir.x() * fluidHDiffMult;
                    flowZ += (float) dir.z() * fluidHDiffMult;
                }
            }
        }
        var flowVec = new MutableVec3d(flowX, 0, flowZ);

        if (isFluid(localBlockState.block()) && (localBlockState.id() - localBlockState.block().minStateId() >= 8)) {
            for (var dir : Direction.HORIZONTALS) {
                var blockState = getBlockState(localBlockState.x() + dir.x(), localBlockState.y(), localBlockState.z() + dir.z());
                var blockStateAbove = getBlockState(localBlockState.x() + dir.x(), localBlockState.y() + 1, localBlockState.z() + dir.z());
                if (blockState.isSolidBlock() || blockStateAbove.isSolidBlock()) {
                    flowVec.normalize();
                    flowVec.add(0, -6, 0);
                    break;
                }
            }
        }
        flowVec.normalize();
        return flowVec;
    }

    private static boolean affectsFlow(BlockState inType, int x, int y, int z) {
        var blockState = getBlockState(x, y, z);
        var inTypeWaterAndBlockIsWaterlogged = inType.block() == BlockRegistry.WATER && BLOCK_DATA.isWaterLogged(blockState.id());
        return !isFluid(blockState.block()) || blockState.block() == inType.block() || inTypeWaterAndBlockIsWaterlogged;
    }


}
