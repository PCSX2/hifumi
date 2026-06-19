// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.database.objects;

public class ScamHashObject {

    private String sha256;
    private long timestamp;
    private String description;
    private boolean active;
    
    public ScamHashObject(String sha256, long timestamp, String description, boolean active) {
        this.sha256 = sha256;
        this.timestamp = timestamp;
        this.description = description;
        this.active = active;
    }

    public String getSHA256() {
        return this.sha256;
    }
    
    public long getTimestamp() {
        return this.timestamp;
    }
    
    public String getDescription() {
        return this.description;
    }
    
    public boolean isActive() {
        return this.active;
    }
}
