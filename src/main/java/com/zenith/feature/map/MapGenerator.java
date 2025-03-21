package com.zenith.feature.map;

import com.zenith.cache.data.chunk.Chunk;
import com.zenith.feature.world.World;
import com.zenith.mc.block.Block;
import com.zenith.util.math.MathHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.SneakyThrows;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.BitStorage;
import org.jspecify.annotations.NonNull;

import static com.zenith.Shared.*;
import static com.zenith.cache.data.chunk.Chunk.chunkPosToLong;

public class MapGenerator {

    @SneakyThrows
    public static byte[] generateMapData() {
        return generateMapData(128, true);
    }

    @SneakyThrows
    public static byte[] generateMapData(final int size) {
        return generateMapData(size, true);
    }

    @SneakyThrows
    public static byte[] generateMapData(final int size, final boolean vanillaAlign) {
        final int chunksSize = size / 16;
        final int dataSize = size * size;
        final int halfWChunks = chunksSize / 2;
        final byte[] data = new byte[dataSize];

        var centerChunkX = CACHE.getChunkCache().getCenterX();
        var centerChunkZ = CACHE.getChunkCache().getCenterZ();

        if (vanillaAlign) {
            // todo: this only works well for 128x128 maps
            final double playerX = CACHE.getPlayerCache().getX();
            final double playerZ = CACHE.getPlayerCache().getZ();
            int playerFlooredX = MathHelper.floorI((playerX + ((double) size / 2)) / size);
            int playerFlooredZ = MathHelper.floorI((playerZ + ((double) size / 2)) / size);
            int centerX = playerFlooredX * size + size / 2 - (size / 2);
            int centerZ = playerFlooredZ * size + size / 2 - (size / 2);
            centerChunkX = centerX / 16;
            centerChunkZ = centerZ / 16;
        }

        final int minChunkX = centerChunkX - halfWChunks;
        final int minChunkZ = centerChunkZ - halfWChunks;
        final int maxChunkX = centerChunkX + halfWChunks;
        final int maxChunkZ = centerChunkZ + halfWChunks;

        final int minBlockX = minChunkX * 16;
        final int minBlockZ = minChunkZ * 16;
        final int maxBlockX = maxChunkX * 16;
        final int maxBlockZ = maxChunkZ * 16;
        final Long2ObjectMap<BitStorage> chunkToHeightMap = generateHeightMapFromChunkData(minChunkX, minChunkZ - 1, maxChunkX, maxChunkZ);

        for (int x = minBlockX; x < maxBlockX; x++) {
            double d0 = 0.0;
            // need to populate 1 above block height in order to calculate brightness correctly
            final int aboveZBlock = minBlockZ - 1;
            final Chunk aboveChunk = CACHE.getChunkCache().get(x >> 4, aboveZBlock >> 4);
            if (aboveChunk != null) {
                final BitStorage heightsStorage = chunkToHeightMap.get(chunkPosToLong(x >> 4, aboveZBlock >> 4));
                if (heightsStorage != null) {
                    d0 = getHeight(x, aboveZBlock, heightsStorage, (aboveChunk.getMinSection() << 4));
                }
            }
            for (int z = minBlockZ; z < maxBlockZ; z++) {
                final int sectionX = x & 15;
                final int sectionZ = z & 15;
                final int chunkX = x >> 4;
                final int chunkZ = z >> 4;
                final Chunk chunk = CACHE.getChunkCache().get(chunkX, chunkZ);
                if (chunk == null) continue;
                final BitStorage heightsStorage = chunkToHeightMap.get(chunkPosToLong(chunkX, chunkZ));
                if (heightsStorage == null) continue;
                final int relChunkX = chunkX - (centerChunkX - halfWChunks);
                final int relChunkZ = chunkZ - (centerChunkZ - halfWChunks);

                int i0 = 0;
                double d1 = 0.0;

                int height = getHeight(x, z, heightsStorage, (chunk.getMinSection() << 4));
                int blockStateId = chunk.getBlockStateId(sectionX, height, sectionZ);
                Block block = BLOCK_DATA.getBlockDataFromBlockStateId(blockStateId);
                int mapColorId = 0;
                if (block != null) mapColorId = block.mapColorId();
                while (mapColorId == 0 && height > chunk.minY()) {
                    blockStateId = chunk.getBlockStateId(sectionX, --height, sectionZ);
                    block = BLOCK_DATA.getBlockDataFromBlockStateId(blockStateId);
                    if (block != null) mapColorId = block.mapColorId();
                }
                if (height > chunk.minY() && World.isWater(block)) {
                    int yUnderBlock = height - 1;
                    int blockStateId2;
                    Block block2;
                    do {
                        blockStateId2 = chunk.getBlockStateId(sectionX, yUnderBlock--, sectionZ);
                        block2 = BLOCK_DATA.getBlockDataFromBlockStateId(blockStateId2);
                        i0++; // water brightness shading
                    } while (yUnderBlock > chunk.minY() && World.isWater(block2));
                }

                d1 += height;

                Brightness brightness;
                if (mapColorId == 12) { // water
                    final double d2 = (double)i0 * 0.1 + (double)(x + z & 1) * 0.2;
                    if (d2 < 0.5) {
                        brightness = Brightness.HIGH;
                    } else if (d2 > 0.9) {
                        brightness = Brightness.LOW;
                    } else {
                        brightness = Brightness.NORMAL;
                    }
                } else {
                    final double d3 = (d1 - d0) * 4.0 / 5.0 + ((double)(x + z & 1) - 0.5) * 0.4;
                    if (d3 > 0.6) {
                        brightness = Brightness.HIGH;
                    } else if (d3 < -0.6) {
                        brightness = Brightness.LOW;
                    } else {
                        brightness = Brightness.NORMAL;
                    }
                }
                d0 = d1;
                final byte packedId = MAP_BLOCK_COLOR.getPackedId(mapColorId, brightness);
                final int rowX = relChunkX * 16 + sectionX;
                final int rowZ = relChunkZ * 16 + sectionZ;
                data[rowX + rowZ * size] = packedId;
            }
        }

        return data;
    }

    @NonNull
    private static Long2ObjectMap<BitStorage> generateHeightMapFromChunkData(final int minChunkX, final int minChunkZ, final int maxChunkX, final int maxChunkZ) {
        final Long2ObjectMap<BitStorage> chunkToHeightMap = new Long2ObjectOpenHashMap<>((maxChunkX - minChunkX) * (maxChunkZ - minChunkZ));
        for (int chunkX = minChunkX; chunkX < maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ < maxChunkZ; chunkZ++) {
                final Chunk chunk = CACHE.getChunkCache().get(chunkX, chunkZ);
                if (chunk == null) continue;
                final BitStorage heightsStorage = generateHeightMapData(chunk);
                chunkToHeightMap.put(chunkPosToLong(chunkX, chunkZ), heightsStorage);
            }
        }
        return chunkToHeightMap;
    }

    private static BitStorage generateHeightMapData(final Chunk chunk) {
        final int minBuildHeight = chunk.minY();
        final int maxBuildHeight = chunk.maxY();
        long[] worldSurfaces = new long[37];
        int bitsPerEntry = MathHelper.log2Ceil((chunk.getMaxSection() << 4) + 1);
        final BitStorage storage = new BitStorage(bitsPerEntry, 256, worldSurfaces);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = maxBuildHeight; y > minBuildHeight; y--) {
                    final int blockStateId = chunk.getBlockStateId(x, y, z);
                    Block block = BLOCK_DATA.getBlockDataFromBlockStateId(blockStateId);
                    if (block != null && !BLOCK_DATA.isAir(block)) {
                        int index = x + z * 16;
                        storage.set(index, y - minBuildHeight);
                        break;
                    }
                }
            }
        }
        return storage;
    }

    private static int getHeight(int blockX, int blockZ, BitStorage data, int minBuildHeight) {
        return getFirstAvailable(blockX & 15, blockZ & 15, data, minBuildHeight);
    }

    private static int getFirstAvailable(int i, BitStorage data, int minBuildHeight) {
        return data.get(i) + minBuildHeight;
    }

    private static int getFirstAvailable(int i, int j, BitStorage data, int minBuildHeight) {
        return getFirstAvailable(getIndex(i, j), data, minBuildHeight);
    }

    private static int getIndex(int i, int j) {
        return i + j * 16;
    }
}
