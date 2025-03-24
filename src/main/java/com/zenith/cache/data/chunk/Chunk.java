package com.zenith.cache.data.chunk;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;
import org.geysermc.mcprotocollib.protocol.data.game.level.HeightmapTypes;
import org.geysermc.mcprotocollib.protocol.data.game.level.LightUpdateData;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class Chunk {
    final int x;
    final int z;
    final ChunkSection[] sections;
    final int maxSection;
    final int minSection;
    final List<BlockEntityInfo> blockEntities;
    LightUpdateData lightUpdateData;

    static final Map<HeightmapTypes, long[]> EMPTY_HEIGHT_MAP = generateEmptyHeightMap();

    static Map<HeightmapTypes, long[]> generateEmptyHeightMap() {
        return new EnumMap<>(HeightmapTypes.class);
    }

    /**
     * Client do not need a valid heightmap for rendering and gameplay to work
     *
     * Also even if we do cache real heightmaps they are not guaranteed to be correct
     * as we do not have logic to rebuild or mutate heightmaps as block updates occur
     */
    public final Map<HeightmapTypes, long[]> getHeightMap() {
        return EMPTY_HEIGHT_MAP;
    }

    public long getChunkPos() {
        return chunkPosToLong(x, z);
    }

    public static long chunkPosToLong(final int x, final int z) {
        return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
    }

    public static int longToChunkX(final long l) {
        return (int) (l & 4294967295L);
    }

    public static int longToChunkZ(final long l) {
        return (int) (l >> 32 & 4294967295L);
    }

    public int getBlockStateId(final int relativeX, final int y, final int relativeZ) {
        final ChunkSection section = getChunkSection(y);
        if (section == null) return 0;
        return section.getBlock(relativeX, y & 15, relativeZ);
    }

    public ChunkSection getChunkSection(final int y) {
        var sectionIndex = getSectionIndex(y);
        if (sectionIndex < 0 || sectionIndex >= sections.length) return null;
        return sections[sectionIndex];
    }

    public int getSectionIndex(final int y) {
        return (y >> 4) - minSection;
    }

    public int minY() {
        return minSection << 4;
    }

    public int maxY() {
        return (maxSection << 4) - 1;
    }

    public int getSectionsCount() {
        return sections.length;
    }
}
