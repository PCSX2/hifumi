package net.pcsx2.hifumi.database.objects;

public class MemberEventObject {

    private long timestamp;
    private long userId;
    private Action action;

    public MemberEventObject(long timestamp, long userId, Action action) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.action = action;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public long getUserId() {
        return this.userId;
    }

    public Action getAction() {
        return this.action;
    }

    public enum Action {
        JOIN,
        LEAVE,
        BAN;
    }
}
