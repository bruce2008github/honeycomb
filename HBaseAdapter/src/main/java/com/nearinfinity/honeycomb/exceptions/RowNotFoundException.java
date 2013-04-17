package com.nearinfinity.honeycomb.exceptions;

import java.util.UUID;

public class RowNotFoundException extends RuntimeException {
    private UUID uuid;

    public RowNotFoundException(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return String.format("RowNotFoundException{uuid=%s}", uuid);
    }
}