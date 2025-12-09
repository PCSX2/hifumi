package net.pcsx2.hifumi.database.objects;

import java.util.Optional;

public class WarezEventObject {

    private long timestamp;
    private long userId;
    private Action action;
    private Long messageId;
    private String content;
    private String messageAction;
    private long attachments;

    public WarezEventObject(long timestamp, long userId, Action action, Long messageId) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.action = action;
        this.messageId = messageId;
    }

    public WarezEventObject(long timestamp, long userId, Action action, Long messageId, String content, String messageAction, long attachments) {
        this.timestamp = timestamp;
        this.userId = userId;
        this.action = action;
        this.messageId = messageId;
        this.content = content;
        this.messageAction = messageAction;
        this.attachments = attachments;
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

    public Optional<Long> getMessageId() {
        return Optional.ofNullable(messageId);
    }

    public Optional<String> getMessageContent() {
        return Optional.ofNullable(content);
    }

    public Optional<String> getMessageAction() {
        return Optional.ofNullable(messageAction);
    }

    public Optional<Long> getMessageAttachmentCount() {
        return Optional.ofNullable(attachments);
    }

    public enum Action {
        ADD,
        REMOVE;
    }
}
