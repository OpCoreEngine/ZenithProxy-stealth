package com.zenith.feature.api.mcstatus.model;

import org.jspecify.annotations.Nullable;

public record MCStatusResponse(
    boolean online,
    String host,
    int port,
    @Nullable String ip_address
) {
}
