package net.pcsx2.hifumi.database.objects;

public class InteractionEventObject {

    private final long eventId;
    private final long timestamp;
    private final long userId;

    public InteractionEventObject(long eventId, long timestamp, long userId) {
        this.eventId = eventId;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public long getEventId() {
        return this.eventId;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getUserId() {
        return this.userId;
    }
}
