// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.event;

import java.time.Instant;
import java.util.UUID;

public abstract class AbstractInteractionElement {

    protected Instant createdInstant;
    protected UUID uuid;
    protected String userId;
    protected String commandName;
    
    public AbstractInteractionElement(String userId, String commandName) {
        this.createdInstant = Instant.now();
        this.userId = userId;
        this.commandName = commandName;
        this.uuid = UUID.nameUUIDFromBytes(new String(userId + commandName).getBytes());
    }
    
    public Instant getCreatedInstant() {
        return createdInstant;
    }
    
    public UUID getUUID() {
        return uuid;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getCommandName() {
        return commandName;
    }
}
