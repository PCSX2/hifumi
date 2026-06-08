package net.pcsx2.hifumi.database.objects;

public class ScamHashObject {

    private String sha256;
    private long timestamp;
    private String description;
    
    public ScamHashObject(String sha256, long timestamp, String description) {
        this.sha256 = sha256;
        this.timestamp = timestamp;
        this.description = description;
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
}
