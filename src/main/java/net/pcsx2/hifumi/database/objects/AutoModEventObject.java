package net.pcsx2.hifumi.database.objects;

import net.dv8tion.jda.api.entities.automod.AutoModResponse;
import net.dv8tion.jda.api.entities.automod.AutoModTriggerType;

public class AutoModEventObject {

    private long userId;
    private long messageId;
    private long channelId;
    private long alertMessageId;
    private long ruleId;
    private long timestamp;
    private String trigger;
    private String content;
    private String matchedContent;
    private String matchedKeyword;
    private String responseType;

    public AutoModEventObject(long userId, long messageId, long channelId, long alertMessageId, long ruleId, long timestamp, String trigger, String content, String matchedContent, String matchedKeyword, String responseType) {
        this.userId = userId;
        this.messageId = messageId;
        this.channelId = channelId;
        this.alertMessageId = alertMessageId;
        this.ruleId = ruleId;
        this.timestamp = timestamp;
        this.trigger = trigger;
        this.content = content;
        this.matchedContent = matchedContent;
        this.matchedKeyword = matchedKeyword;
        this.responseType = responseType;
    }

    public long getUserId() {
        return this.userId;
    }

    public long getMessageId() {
        return this.messageId;
    }

    public long getChannelId() {
        return this.channelId;
    }

    public long getAlertMessageId() {
        return this.alertMessageId;
    }

    public long getRuleId() {
        return this.ruleId;
    }
    
    public long getTimestamp() {
        return this.timestamp;
    }

    public AutoModTriggerType getTriggerType() {
        return AutoModTriggerType.valueOf(this.trigger);
    }

    public String getContent() {
        return this.content;
    }

    public String getMatchedContent() {
        return this.matchedContent;
    }

    public String getMatchedKeyword() {
        return this.matchedKeyword;
    }

    public AutoModResponse.Type getResponseType() {
        return AutoModResponse.Type.valueOf(this.responseType);
    }
}
