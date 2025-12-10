package net.pcsx2.hifumi.database.objects;

public class CommandEventObject {

    private long eventId;
    private long commandId;
    private long userId;
    private long timestamp;

    public CommandEventObject(long eventId, long commandId, long userId, long timestamp) {
        this.eventId = eventId;
        this.commandId = commandId;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public long getEventId() {
        return this.eventId;
    }

    public long getCommandId() {
        return this.commandId;
    }

    public long getUserId() {
        return this.userId;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}
