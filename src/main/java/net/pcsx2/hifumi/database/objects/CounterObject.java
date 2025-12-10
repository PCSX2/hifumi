package net.pcsx2.hifumi.database.objects;

public class CounterObject {

    private String type;
    private long timestamp;
    private long eta;

    public CounterObject(String type, long timestamp, long eta) {
        this.type = type;
        this.timestamp = timestamp;
        this.eta = eta;
    }

    public String getType() {
        return this.type;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getValue() {
        return this.eta;
    }
}
