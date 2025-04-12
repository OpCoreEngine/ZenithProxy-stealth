package com.zenith.mc.dimension;

import com.zenith.util.struct.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DimensionDataManager {
    private final Map<String, DimensionData> dimensionNameToData = new ConcurrentHashMap<>(DimensionRegistry.REGISTRY.size(), Maps.FAST_LOAD_FACTOR);

    public DimensionDataManager() {
        init();
    }

    private void init() {
        for (var entry : DimensionRegistry.REGISTRY.getIdMap().int2ObjectEntrySet()) {
            dimensionNameToData.put(entry.getValue().name(), entry.getValue());
        }
    }

    public DimensionData getDimensionData(final int id) {
        return DimensionRegistry.REGISTRY.get(id);
    }

    public DimensionData getDimensionData(final String name) {
        return dimensionNameToData.get(name);
    }

    public Collection<String> dimensionNames() {
        return dimensionNameToData.keySet();
    }
}
