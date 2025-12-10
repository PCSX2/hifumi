package net.pcsx2.hifumi.database.objects;

import java.time.OffsetDateTime;

public class AttachmentObject {

    private String discordId;
    private OffsetDateTime created;
    private String messageId;
    private String name;
    private String contentType;
    private String proxyUrl;

    public AttachmentObject(String discordId, OffsetDateTime created, String messageId, String name, String contentType, String proxyUrl) {
        this.discordId = discordId;
        this.created = created;
        this.messageId = messageId;
        this.name = name;
        this.contentType = contentType;
        this.proxyUrl = proxyUrl;
    }

    public String getDiscordId() {
        return this.discordId;
    }

    public OffsetDateTime getCreatedTime() {
        return this.created;
    }

    public String getMessageId() {
        return this.messageId;
    }

    public String getName() {
        return this.name;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getProxyUrl() {
        return this.proxyUrl;
    }
}
