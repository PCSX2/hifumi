package net.pcsx2.hifumi.database.objects;

import java.time.OffsetDateTime;
import java.util.ArrayList;

/**
 * Representation of a message, constructed by supplying old message information fetched from SQL.
 * Intended for scenarios where we need information on the old contents of an edited or deleted message.
 */
public class MessageObject {

    private long messageId;
    private long authorId;
    private long channelId;
    private OffsetDateTime created;
    private OffsetDateTime modified;
    private String bodyContent;
    private String jumpUrl;
    private String referencedMessageId;
    private ArrayList<AttachmentObject> attachments;

    public MessageObject(long messageId, long authorId, OffsetDateTime created, OffsetDateTime modified, long channelId, String bodyContent, String jumpUrl, String referencedMessageId, ArrayList<AttachmentObject> attachments) {
        this.messageId = messageId;
        this.authorId = authorId;
        this.created = created;
        this.modified = modified;
        this.channelId = channelId;
        this.bodyContent = bodyContent;
        this.jumpUrl = jumpUrl;
        this.referencedMessageId = referencedMessageId;
        this.attachments = attachments;
    }

    public long getMessageId() {
        return this.messageId;
    }

    public long getAuthorId() {
        return this.authorId;
    }

    public long getChannelId() {
        return this.channelId;
    }

    public OffsetDateTime getCreatedTime() {
        return this.created;
    }

    public OffsetDateTime getModifiedTime() {
        return this.modified;
    }

    public String getBodyContent() {
        return this.bodyContent;
    }

    public String getJumpUrl() {
        return this.jumpUrl;
    }

    public String getReferencedMessageId() {
        return this.referencedMessageId;
    }

    public ArrayList<AttachmentObject> getAttachments() {
        return this.attachments;
    }
}
