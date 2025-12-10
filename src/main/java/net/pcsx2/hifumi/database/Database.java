package net.pcsx2.hifumi.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.charting.AutomodChartData;
import net.pcsx2.hifumi.charting.MemberChartData;
import net.pcsx2.hifumi.charting.WarezChartData;
import net.pcsx2.hifumi.database.objects.AttachmentObject;
import net.pcsx2.hifumi.database.objects.AutoModEventObject;
import net.pcsx2.hifumi.database.objects.CommandEventObject;
import net.pcsx2.hifumi.database.objects.CounterObject;
import net.pcsx2.hifumi.database.objects.InteractionEventObject;
import net.pcsx2.hifumi.database.objects.MemberEventObject;
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.database.objects.WarezEventObject;
import net.pcsx2.hifumi.util.DateTimeUtils;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.TimeUtils;
import net.pcsx2.hifumi.util.UserUtils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.automod.AutoModResponse;
import net.dv8tion.jda.api.events.automod.AutoModExecutionEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class Database {

    /**
     * Store user, channel, message, attachment, and event records
     */
    public static void insertMessage(Message message, boolean skipEvent) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, message.getAuthor().getIdLong());
            insertUser.setLong(2, message.getAuthor().getTimeCreated().toEpochSecond());
            insertUser.setString(3, message.getAuthor().getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertChannel = conn.prepareStatement("""
                    INSERT INTO channel (discord_id, name)
                    VALUES (?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertChannel.setLong(1, message.getChannel().getIdLong());
            insertChannel.setString(2, message.getChannel().getName());
            insertChannel.executeUpdate();
            insertChannel.close();

            // Check if the referenced message exists in the database; if not, try to add it first.
            if (message.getReferencedMessage() != null) {
                if (Database.getLatestMessage(message.getReferencedMessage().getIdLong()) == null) {
                    Database.insertMessage(message.getReferencedMessage(), skipEvent);
                }
            }

            PreparedStatement insertMessage = conn.prepareStatement("""
                    INSERT INTO message (message_id, fk_channel, jump_link, fk_reply_to_message, timestamp, fk_user)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT (message_id) DO NOTHING;
                    """);
            insertMessage.setLong(1, message.getIdLong());
            insertMessage.setLong(2, message.getChannel().getIdLong());
            insertMessage.setString(3, message.getJumpUrl());

            if (message.getReferencedMessage() != null) {
                insertMessage.setLong(4, message.getReferencedMessage().getIdLong());
            } else {
                insertMessage.setNull(4, Types.BIGINT);
            }
            
            insertMessage.setLong(5, message.getTimeCreated().toEpochSecond());
            insertMessage.setLong(6, message.getAuthor().getIdLong());
            insertMessage.executeUpdate();
            insertMessage.close();

            if (!skipEvent) {
                PreparedStatement insertEvent = conn.prepareStatement("""
                        INSERT INTO message_event (fk_user, fk_message, timestamp, action, content)
                        VALUES (?, ?, ?, ?, ?);
                        """);
                insertEvent.setLong(1, message.getAuthor().getIdLong());
                insertEvent.setLong(2, message.getIdLong());
                insertEvent.setLong(3, message.getTimeCreated().toEpochSecond());
                insertEvent.setString(4, "send");
                insertEvent.setString(5, message.getContentRaw());
                insertEvent.executeUpdate();
                insertEvent.close();
                
                // Check if this message had any attachments. No need to continue if not.
                List<Attachment> attachments = message.getAttachments();

                if (!attachments.isEmpty()) {
                    PreparedStatement insertAttachments = conn.prepareStatement("""
                            INSERT INTO message_attachment (discord_id, timestamp, fk_message, content_type, proxy_url, filename)
                            VALUES (?, ?, ?, ?, ?, ?);
                            """);

                    for (Attachment attachment : attachments) {
                        insertAttachments.setLong(1, attachment.getIdLong());
                        insertAttachments.setLong(2, attachment.getTimeCreated().toEpochSecond());
                        insertAttachments.setLong(3, message.getIdLong());
                        insertAttachments.setString(4, attachment.getContentType());
                        insertAttachments.setString(5, attachment.getProxyUrl());
                        insertAttachments.setString(6, attachment.getFileName());
                        insertAttachments.addBatch();
                    }
                    
                    insertAttachments.executeBatch();
                    insertAttachments.close();
                }
            }
        } catch (SQLException e) {
             Messaging.logException("Database", "insertMessageReceivedEvent", e);
        }
    }

    /**
     * Store channel, message and event records
     * @param event
     */
    public static void insertMessageDeleteEvent(MessageDeleteEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        Connection conn = null;
        long userId = 0;

        try {
            conn = HifumiBot.getSelf().getSQLite().getConnection();

            PreparedStatement getUser = conn.prepareStatement("""
                    SELECT fk_user, fk_message
                    FROM message_event
                    WHERE fk_message = ?
                    LIMIT 1;
                    """);
            getUser.setLong(1, event.getMessageIdLong());
            ResultSet res = getUser.executeQuery();

            if (res.next()) {
                userId = res.getLong("fk_user");
            }
            
            getUser.close();

            PreparedStatement insertChannel = conn.prepareStatement("""
                    INSERT INTO channel (discord_id, name)
                    VALUES (?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertChannel.setLong(1, event.getChannel().getIdLong());
            insertChannel.setString(2, event.getChannel().getName());
            insertChannel.executeUpdate();
            insertChannel.close();

            PreparedStatement insertMessage = conn.prepareStatement("""
                    INSERT INTO message (message_id, fk_channel)
                    VALUES (?, ?)
                    ON CONFLICT (message_id) DO NOTHING;
                    """);
            insertMessage.setLong(1, event.getMessageIdLong());
            insertMessage.setLong(2, event.getChannel().getIdLong());
            insertMessage.executeUpdate();
            insertMessage.close();

            PreparedStatement insertEvent = conn.prepareStatement("""
                    INSERT INTO message_event (fk_user, fk_message, timestamp, action)
                    VALUES (?, ?, ?, ?);
                    """);
            insertEvent.setLong(1, userId);
            insertEvent.setLong(2, event.getMessageIdLong());
            insertEvent.setLong(3, now.toEpochSecond());
            insertEvent.setString(4, "delete");
            insertEvent.executeUpdate();
            insertEvent.close();
        } catch (SQLException e) {
             Messaging.logException("Database", "insertMessageDeleteEvent", e);
        }
    }

    /**
     * Store channel, message and event records
     * @param event
     */
    public static void insertMessageBulkDeleteEvent(MessageBulkDeleteEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        
        for (String messageId : event.getMessageIds()) {
            Connection conn = null;
            long userId = 0;

            try {
                conn = HifumiBot.getSelf().getSQLite().getConnection();

                PreparedStatement getUser = conn.prepareStatement("""
                        SELECT fk_user, fk_message
                        FROM message_event
                        WHERE fk_message = ?
                        LIMIT 1;
                        """);
                getUser.setLong(1, Long.valueOf(messageId));
                ResultSet res = getUser.executeQuery();

                if (res.next()) {
                    userId = res.getLong("fk_user");
                }
                
                getUser.close();

                PreparedStatement insertChannel = conn.prepareStatement("""
                        INSERT INTO channel (discord_id, name)
                        VALUES (?, ?)
                        ON CONFLICT (discord_id) DO NOTHING;
                        """);
                insertChannel.setLong(1, event.getChannel().getIdLong());
                insertChannel.setString(2, event.getChannel().getName());
                insertChannel.executeUpdate();
                insertChannel.close();

                PreparedStatement insertMessage = conn.prepareStatement("""
                        INSERT INTO message (message_id, fk_channel)
                        VALUES (?, ?)
                        ON CONFLICT (message_id) DO NOTHING;
                        """);
                insertMessage.setLong(1, Long.valueOf(messageId));
                insertMessage.setLong(2, event.getChannel().getIdLong());
                insertMessage.executeUpdate();
                insertMessage.close();

                PreparedStatement insertEvent = conn.prepareStatement("""
                        INSERT INTO message_event (fk_user, fk_message, timestamp, action)
                        VALUES (?, ?, ?, ?);
                        """);
                insertEvent.setLong(1, userId);
                insertEvent.setLong(2, Long.valueOf(messageId));
                insertEvent.setLong(3, now.toEpochSecond());
                insertEvent.setString(4, "delete");
                insertEvent.executeUpdate();
                insertEvent.close();
            } catch (SQLException e) {
                Messaging.logException("Database", "insertMessageBulkDeleteEvent", e);
            }
        }
    }

    /**
     * Store user, channel, message, attachment, and event records
     * @param event
     */
    public static void insertMessageUpdateEvent(MessageUpdateEvent event) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, event.getAuthor().getIdLong());
            insertUser.setLong(2, event.getAuthor().getTimeCreated().toEpochSecond());
            insertUser.setString(3, event.getAuthor().getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertChannel = conn.prepareStatement("""
                    INSERT INTO channel (discord_id, name)
                    VALUES (?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertChannel.setLong(1, event.getChannel().getIdLong());
            insertChannel.setString(2, event.getChannel().getName());
            insertChannel.executeUpdate();
            insertChannel.close();

            PreparedStatement insertMessage = conn.prepareStatement("""
                    INSERT INTO message (message_id, fk_channel, fk_user)
                    VALUES (?, ?, ?)
                    ON CONFLICT (message_id) DO NOTHING;
                    """);
            insertMessage.setLong(1, event.getMessageIdLong());
            insertMessage.setLong(2, event.getChannel().getIdLong());
            insertMessage.setLong(3, event.getAuthor().getIdLong());
            insertMessage.executeUpdate();
            insertMessage.close();

            List<Attachment> attachments = event.getMessage().getAttachments();

            if (!attachments.isEmpty()) {
                PreparedStatement insertAttachment = conn.prepareStatement("""
                        INSERT INTO message_attachment (discord_id, timestamp, fk_message, content_type, proxy_url)
                        VALUES (?, ?, ?, ?, ?)
                        ON CONFLICT (discord_id) DO NOTHING;
                        """);

                for (Attachment attachment : attachments) {
                    insertAttachment.setLong(1, attachment.getIdLong());
                    insertAttachment.setLong(2, attachment.getTimeCreated().toEpochSecond());
                    insertAttachment.setLong(3, event.getMessageIdLong());
                    insertAttachment.setString(4, attachment.getContentType());
                    insertAttachment.setString(5, attachment.getProxyUrl());
                    insertAttachment.addBatch();
                }
                
                insertAttachment.executeBatch();
                insertAttachment.close();
            }

            PreparedStatement insertEvent = conn.prepareStatement("""
                    INSERT INTO message_event (fk_user, fk_message, timestamp, action, content)
                    VALUES (?, ?, ?, ?, ?);
                    """);
            insertEvent.setLong(1, event.getAuthor().getIdLong());
            insertEvent.setLong(2, event.getMessageIdLong());
            insertEvent.setLong(3, (event.getMessage().getTimeEdited() != null ? event.getMessage().getTimeEdited() : event.getMessage().getTimeCreated()).toEpochSecond());
            insertEvent.setString(4, "edit");
            insertEvent.setString(5, event.getMessage().getContentRaw());
            insertEvent.executeUpdate();
            insertEvent.close();
        } catch (SQLException e) {
             Messaging.logException("Database", "insertMessageUpdateEvent", e);
        }
    }

    public static MessageObject getOriginalMessage(String messageId) {
        return Database.getOriginalMessage(Long.valueOf(messageId));
    }

    public static MessageObject getOriginalMessage(long messageIdLong) {
        MessageObject ret = null;
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the original sent message
            PreparedStatement getSendEvent = conn.prepareStatement("""
                    SELECT
                        e.id, e.fk_user, e.fk_message, e.content, e.timestamp,
                        m.fk_channel, m.jump_link, m.fk_reply_to_message
                    FROM message_event AS e
                    INNER JOIN message AS m ON e.fk_message = m.message_id
                    WHERE e.fk_message = ?
                    AND e.action = 'send'
                    ORDER BY m.timestamp ASC
                    LIMIT 1;
                    """);
            getSendEvent.setLong(1, messageIdLong);
            ResultSet originalSendEvent = getSendEvent.executeQuery();

            // If we got a hit...
            if (originalSendEvent.next()) {
                // ... then look for attachments
                PreparedStatement getAttachments = conn.prepareStatement("""
                        SELECT discord_id, timestamp, fk_message, content_type, proxy_url, filename
                        FROM message_attachment
                        WHERE fk_message = ?;
                        """);
                getAttachments.setLong(1, messageIdLong);
                ResultSet attachments = getAttachments.executeQuery();

                ArrayList<AttachmentObject> attachmentList = new ArrayList<AttachmentObject>();

                while (attachments.next()) {
                    AttachmentObject attachment = new AttachmentObject(
                        String.valueOf(attachments.getLong("discord_id")),
                        DateTimeUtils.longToOffsetDateTime(attachments.getLong("created")), 
                        String.valueOf(messageIdLong),
                        attachments.getString("filename"),
                        attachments.getString("content_type"),
                        attachments.getString("proxy_url")
                    );

                    attachmentList.add(attachment);
                }

                ret = new MessageObject(
                    messageIdLong, 
                    originalSendEvent.getLong("fk_user"), 
                    DateTimeUtils.longToOffsetDateTime(originalSendEvent.getLong("timestamp")), 
                    null, 
                    originalSendEvent.getLong("fk_channel"),
                    originalSendEvent.getString("content"),
                    originalSendEvent.getString("jump_link"), 
                    originalSendEvent.getString("fk_reply_to_message"), 
                    attachmentList
                );
            }

            getSendEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getOriginalMessage", e);
        }
        
        return ret;
    }

    public static MessageObject getLatestMessage(String messageId) {
        return Database.getLatestMessage(Long.valueOf(messageId));
    }

    public static MessageObject getLatestMessage(long messageIdLong) {
        MessageObject ret = null;
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getMessageEvent = conn.prepareStatement("""
                    SELECT
                        e.id, e.fk_user, e.fk_message, e.content, e.timestamp, e.action,
                        m.fk_channel, m.jump_link, m.fk_reply_to_message
                    FROM message_event AS e
                    INNER JOIN message AS m ON e.fk_message = m.message_id
                    WHERE e.fk_message = ?
                    AND (
                        e.action = 'send'
                        OR e.action = 'edit'
                    )
                    ORDER BY e.timestamp DESC
                    LIMIT 1;
                    """);
            getMessageEvent.setLong(1, messageIdLong);
            ResultSet latestEvent = getMessageEvent.executeQuery();

            // If we got a hit...
            if (latestEvent.next()) {
                // ... then look for attachments
                PreparedStatement getAttachments = conn.prepareStatement("""
                        SELECT discord_id, timestamp, fk_message, content_type, proxy_url, filename
                        FROM message_attachment
                        WHERE fk_message = ?;
                        """);
                getAttachments.setLong(1, messageIdLong);
                ResultSet attachments = getAttachments.executeQuery();

                ArrayList<AttachmentObject> attachmentList = new ArrayList<AttachmentObject>();

                while (attachments.next()) {
                    AttachmentObject attachment = new AttachmentObject(
                        String.valueOf(attachments.getLong("discord_id")),
                        DateTimeUtils.longToOffsetDateTime(attachments.getLong("timestamp")), 
                        String.valueOf(messageIdLong),
                        attachments.getString("filename"),
                        attachments.getString("content_type"),
                        attachments.getString("proxy_url")
                    );

                    attachmentList.add(attachment);
                }

                String action = latestEvent.getString("action");

                ret = new MessageObject(
                    messageIdLong, 
                    latestEvent.getLong("fk_user"), 
                    DateTimeUtils.longToOffsetDateTime(latestEvent.getLong("timestamp")),
                    (action != null && action.equals("edit") ? DateTimeUtils.longToOffsetDateTime(latestEvent.getLong("timestamp")) : null), 
                    latestEvent.getLong("fk_channel"),
                    latestEvent.getString("content"),
                    latestEvent.getString("jump_link"), 
                    latestEvent.getString("fk_reply_to_message"), 
                    attachmentList
                );
            }

            getMessageEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getLatestMessage", e);
        }
        
        return ret;
    }

    public static ArrayList<MessageObject> getAllMessageRevisions(long messageIdLong) {
        ArrayList<MessageObject> ret = new ArrayList<MessageObject>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getMessageEvent = conn.prepareStatement("""
                    SELECT
                        e.id, e.fk_user, e.fk_message, e.content, e.timestamp AS e_timestamp, e.action,
                        m.fk_channel, m.jump_link, m.fk_reply_to_message, m.timestamp AS m_timestamp
                    FROM message_event AS e
                    INNER JOIN message AS m ON e.fk_message = m.message_id
                    WHERE e.fk_message = ?
                    AND (
                        e.action = 'send'
                        OR e.action = 'edit'
                    )
                    ORDER BY e.timestamp DESC;
                    """);
            getMessageEvent.setLong(1, messageIdLong);
            ResultSet latestEvent = getMessageEvent.executeQuery();

            // If we got a hit...
            while (latestEvent.next()) {
                // ... then look for attachments
                PreparedStatement getAttachments = conn.prepareStatement("""
                        SELECT discord_id, timestamp, fk_message, content_type, proxy_url, filename
                        FROM message_attachment
                        WHERE fk_message = ?;
                        """);
                getAttachments.setLong(1, messageIdLong);
                ResultSet attachments = getAttachments.executeQuery();

                ArrayList<AttachmentObject> attachmentList = new ArrayList<AttachmentObject>();

                while (attachments.next()) {
                    AttachmentObject attachment = new AttachmentObject(
                        String.valueOf(attachments.getLong("discord_id")),
                        DateTimeUtils.longToOffsetDateTime(attachments.getLong("timestamp")), 
                        String.valueOf(messageIdLong),
                        attachments.getString("filename"),
                        attachments.getString("content_type"),
                        attachments.getString("proxy_url")
                    );

                    attachmentList.add(attachment);
                }

                String action = latestEvent.getString("action");

                ret.add(new MessageObject(
                    messageIdLong, 
                    latestEvent.getLong("fk_user"), 
                    DateTimeUtils.longToOffsetDateTime(latestEvent.getLong("m_timestamp")),
                    (action != null && action.equals("edit") ? DateTimeUtils.longToOffsetDateTime(latestEvent.getLong("e_timestamp")) : null), 
                    latestEvent.getLong("fk_channel"),
                    latestEvent.getString("content"),
                    latestEvent.getString("jump_link"), 
                    latestEvent.getString("fk_reply_to_message"), 
                    attachmentList
                ));
            }

            getMessageEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getAllMessageRevisions", e);
        }
        
        return ret;
    }

    public static ArrayList<MessageObject> getIdenticalMessagesSinceTime(long userIdLong, String contentRaw, long timestamp) {
        ArrayList<MessageObject> ret = new ArrayList<MessageObject>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getMessageEvents = conn.prepareStatement("""
                    SELECT
                        e.id, e.fk_user, e.fk_message, e.content, e.timestamp,
                        m.fk_channel, m.jump_link, m.fk_reply_to_message
                    FROM message_event AS e
                    INNER JOIN message AS m ON e.fk_message = m.message_id
                    WHERE e.fk_user = ?
                    AND e.content = ?
                    AND e.action = 'send'
                    AND e.timestamp >= ?
                    ORDER BY e.timestamp DESC;
                    """);
            getMessageEvents.setLong(1, userIdLong);
            getMessageEvents.setString(2, contentRaw);
            getMessageEvents.setLong(3, timestamp);
            ResultSet res = getMessageEvents.executeQuery();

            while (res.next()) {
                MessageObject messageObj = new MessageObject(
                    res.getLong("fk_message"),
                    res.getLong("fk_user"),
                    DateTimeUtils.longToOffsetDateTime(res.getLong("timestamp")),
                    null,
                    res.getLong("fk_channel"),
                    res.getString("content"),
                    res.getString("jump_link"),
                    null,
                    null
                );

                ret.add(messageObj);
            }

            getMessageEvents.close();
            return ret;
        } catch (SQLException e) {
            Messaging.logException("Database", "getIdenticalMessagesSinceTime", e);
        }
        
        return ret;
    }

    public static MessageObject getIdenticalMessageSinceTimeInOtherChannel(long userIdLong, String contentRaw, long timestamp, long channelIdLong) {
        MessageObject ret = null;
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getMessageEvents = conn.prepareStatement("""
                    SELECT
                        e.id, e.fk_user, e.fk_message, e.content, e.timestamp,
                        m.fk_channel, m.jump_link, m.fk_reply_to_message
                    FROM message_event AS e
                    INNER JOIN message AS m ON e.fk_message = m.message_id
                    WHERE e.fk_user = ?
                    AND e.content = ?
                    AND e.action = 'send'
                    AND e.timestamp >= ?
                    AND e.fk_message NOT IN (
                        SELECT fk_message
                        FROM message_event
                        WHERE fk_message = e.fk_message
                        AND action = 'delete'
                    )
                    AND NOT m.fk_channel = ?
                    ORDER BY e.timestamp DESC
                    LIMIT 1;
                    """);
            getMessageEvents.setLong(1, userIdLong);
            getMessageEvents.setString(2, contentRaw);
            getMessageEvents.setLong(3, timestamp);
            getMessageEvents.setLong(4, channelIdLong);
            ResultSet res = getMessageEvents.executeQuery();

            if (res.next()) {
                ret = new MessageObject(
                    res.getLong("fk_message"),
                    res.getLong("fk_user"),
                    DateTimeUtils.longToOffsetDateTime(res.getLong("timestamp")),
                    null,
                    res.getLong("fk_channel"),
                    res.getString("content"),
                    res.getString("jump_link"),
                    null,
                    null
                );
            }

            getMessageEvents.close();
            return ret;
        } catch (SQLException e) {
            Messaging.logException("Database", "getIdenticalMessageSinceTimeInOtherChannel", e);
        }
        
        return ret;
    }

    public static ArrayList<MessageObject> getAllMessagesSinceTime(long userIdLong, long timestamp) {
        ArrayList<MessageObject> ret = new ArrayList<MessageObject>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getMessageEvents = conn.prepareStatement("""
                    SELECT
                        e.id, e.fk_user, e.fk_message, e.content, e.timestamp,
                        m.fk_channel, m.jump_link, m.fk_reply_to_message
                    FROM message_event AS e
                    INNER JOIN message AS m ON e.fk_message = m.message_id
                    WHERE e.fk_user = ?
                    AND e.action = 'send'
                    AND e.timestamp >= ?
                    ORDER BY e.timestamp DESC;
                    """);
            getMessageEvents.setLong(1, userIdLong);
            getMessageEvents.setLong(2, timestamp);
            ResultSet res = getMessageEvents.executeQuery();

            while (res.next()) {
                MessageObject messageObj = new MessageObject(
                    res.getLong("fk_message"),
                    res.getLong("fk_user"),
                    DateTimeUtils.longToOffsetDateTime(res.getLong("timestamp")),
                    null,
                    res.getLong("fk_channel"),
                    res.getString("content"),
                    res.getString("jump_link"),
                    null,
                    null
                );

                ret.add(messageObj);
            }

            getMessageEvents.close();
            return ret;
        } catch (SQLException e) {
            Messaging.logException("Database", "getAllMessagesSinceTime", e);
        }
        
        return ret;
    }

    public static boolean insertWarezEvent(WarezEventObject warezEvent) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            User usr = HifumiBot.getSelf().getJDA().retrieveUserById(warezEvent.getUserId()).complete();

            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, warezEvent.getUserId());
            insertUser.setLong(2, usr.getTimeCreated().toEpochSecond());
            insertUser.setString(3, usr.getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertWarez = conn.prepareStatement("""
                    INSERT INTO warez_event (timestamp, fk_user, action, fk_message)
                    VALUES (?, ?, ?, ?);
                    """);
            insertWarez.setLong(1, warezEvent.getTimestamp());
            insertWarez.setLong(2, warezEvent.getUserId());
            insertWarez.setString(3, warezEvent.getAction().toString().toLowerCase());

            if (warezEvent.getMessageId().isPresent()) {
                insertWarez.setLong(4, warezEvent.getMessageId().get());
            } else {
                insertWarez.setNull(4, Types.BIGINT);
            }
            
            insertWarez.executeUpdate();
            insertWarez.close();

            return true;
        } catch (SQLException e) {
            Messaging.logException("Database", "insertWarezEvent", e);
        }

        return false;
    }

    public static Optional<WarezEventObject> getLatestWarezAction(long userIdLong) {
        Optional<WarezEventObject> ret = Optional.empty();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement getWarezEvent = conn.prepareStatement("""
                    SELECT timestamp, fk_user, action, fk_message
                    FROM warez_event
                    WHERE fk_user = ?
                    ORDER BY timestamp DESC
                    LIMIT 1;
                    """);
            getWarezEvent.setLong(1, userIdLong);
            ResultSet latestEvent = getWarezEvent.executeQuery();

            if (latestEvent.next()) {
                ret = Optional.of(
                    new WarezEventObject(
                        latestEvent.getLong("timestamp"), 
                        latestEvent.getLong("fk_user"), 
                        WarezEventObject.Action.valueOf(latestEvent.getString("action").toUpperCase()),
                        latestEvent.getLong("fk_message")
                    )
                );
            }

            getWarezEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getLatestWarezAction", e);
        }
        
        return ret;
    }

    public static ArrayList<WarezEventObject> getAllWarezActionsForUser(long userIdLong) {
        ArrayList<WarezEventObject> ret = new ArrayList<WarezEventObject>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement getWarezEvents = conn.prepareStatement("""
                    SELECT e.timestamp, e.fk_user, e.action, e.fk_message, m.content, m.action AS message_action, COUNT(a.discord_id) AS attachments
                    FROM warez_event AS e
                    LEFT JOIN message_event AS m ON e.fk_message = m.fk_message
                    LEFT JOIN message_attachment AS a ON e.fk_message = a.fk_message
                    WHERE e.fk_user = ?
                    GROUP BY e.id
                    ORDER BY e.timestamp DESC;
                    """);
            getWarezEvents.setLong(1, userIdLong);
            ResultSet warezEvent = getWarezEvents.executeQuery();

            while (warezEvent.next()) {
                ret.add(new WarezEventObject(
                    warezEvent.getLong("timestamp"), 
                    warezEvent.getLong("fk_user"), 
                    WarezEventObject.Action.valueOf(warezEvent.getString("action").toUpperCase()),
                    warezEvent.getLong("fk_message"),
                    warezEvent.getString("content"),
                    warezEvent.getString("message_action"),
                    warezEvent.getLong("attachments")
                ));
            }

            getWarezEvents.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getAllWarezActionsForUser", e);
        }
        
        return ret;
    }

    public static ArrayList<ArrayList<WarezEventObject>> getAllWarezActionsForUserPaginated(long userIdLong) {
        ArrayList<ArrayList<WarezEventObject>> ret = new ArrayList<ArrayList<WarezEventObject>>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            boolean isEmpty = false;
            long timestamp = OffsetDateTime.now().toEpochSecond();
            int rowsReturned = 0;

            do { 
                PreparedStatement getWarezEvents = conn.prepareStatement("""
                    SELECT e.timestamp, e.fk_user, e.action, e.fk_message, m.content, m.action AS message_action, COUNT(a.discord_id) AS attachments
                    FROM warez_event AS e
                    LEFT JOIN message_event AS m ON e.fk_message = m.fk_message
                    LEFT JOIN message_attachment AS a ON e.fk_message = a.fk_message
                    WHERE e.fk_user = ?
                    AND e.timestamp < ?
                    GROUP BY e.id
                    ORDER BY e.timestamp DESC
                    LIMIT 10
                    OFFSET ?;
                    """);
                getWarezEvents.setLong(1, userIdLong);
                getWarezEvents.setLong(2, timestamp);
                getWarezEvents.setInt(3, rowsReturned);
                ResultSet warezEvent = getWarezEvents.executeQuery();
                ArrayList<WarezEventObject> warezEvents = new ArrayList<WarezEventObject>();

                while (warezEvent.next()) {
                    WarezEventObject event = new WarezEventObject(
                        warezEvent.getLong("timestamp"), 
                        warezEvent.getLong("fk_user"), 
                        WarezEventObject.Action.valueOf(warezEvent.getString("action").toUpperCase()),
                        warezEvent.getLong("fk_message"),
                        warezEvent.getString("content"),
                        warezEvent.getString("message_action"),
                        warezEvent.getLong("attachments")
                    );
                    warezEvents.add(event);
                }

                getWarezEvents.close();
                rowsReturned += warezEvents.size();

                if (!warezEvents.isEmpty()) {
                    ret.add(warezEvents);
                } else {
                    isEmpty = true;
                }
            } while (!isEmpty);
        } catch (SQLException e) {
            Messaging.logException("Database", "getAllWarezActionsForUserPaginated", e);
        }
        
        return ret;
    }

    public static ArrayList<WarezChartData> getWarezAssignmentsBetween(long startTimestamp, long endTimestamp, String timeUnit) {
        ArrayList<WarezChartData> ret = new ArrayList<WarezChartData>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();
        String formatStr = TimeUtils.getSQLFormatStringFromTimeUnit(timeUnit);
        
        try {
            PreparedStatement getWarezEvent = conn.prepareStatement("""
                    SELECT COUNT(timestamp) AS events, STRFTIME(?, DATETIME(timestamp, 'unixepoch')) AS timeUnit, action
                    FROM warez_event
                    WHERE timestamp >= ?
                    AND timestamp <= ?
                    GROUP BY STRFTIME(?, DATETIME(timestamp, 'unixepoch')), action
                    ORDER BY action ASC, timestamp ASC;
                    """);
            getWarezEvent.setString(1, formatStr);
            getWarezEvent.setLong(2, startTimestamp);
            getWarezEvent.setLong(3, endTimestamp);
            getWarezEvent.setString(4, formatStr);
            ResultSet latestEvent = getWarezEvent.executeQuery();

            while (latestEvent.next()) {
                WarezChartData data = new WarezChartData();
                data.timeUnit = latestEvent.getString("timeUnit");
                data.events = latestEvent.getInt("events");
                data.action = latestEvent.getString("action");
                ret.add(data);
            }

            getWarezEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getWarezAssignmentsSince", e);
        }
        
        return ret;
    }

    public static void insertMemberJoinEvent(GuildMemberJoinEvent event) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {            
            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, event.getMember().getIdLong());
            insertUser.setLong(2, event.getMember().getTimeCreated().toEpochSecond());
            insertUser.setString(3, event.getUser().getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertEvent = conn.prepareStatement("""
                    INSERT INTO member_event (timestamp, fk_user, action)
                    VALUES (?, ?, ?);
                    """);
            insertEvent.setLong(1, event.getGuild().retrieveMemberById(event.getMember().getId()).complete().getTimeJoined().toEpochSecond());
            insertEvent.setLong(2, event.getMember().getIdLong());
            insertEvent.setString(3, "join");
            insertEvent.executeUpdate();
            insertEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "insertMemberJoinEvent", e);
        }
    }

    public static ArrayList<MemberEventObject> getRecentMemberEvents(long userId) {
        ArrayList<MemberEventObject> ret = new ArrayList<MemberEventObject>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement events = conn.prepareStatement("""
                    SELECT timestamp, fk_user, action
                    FROM member_event
                    WHERE fk_user = ?
                    ORDER BY timestamp DESC
                    LIMIT 10;
                    """);
            events.setLong(1, userId);
            ResultSet eventsRes = events.executeQuery();

            while (eventsRes.next()) {
                MemberEventObject event = new MemberEventObject(
                    eventsRes.getLong("timestamp"), 
                    eventsRes.getLong("fk_user"), 
                    MemberEventObject.Action.valueOf(eventsRes.getString("action").toUpperCase())
                );
                ret.add(event);
            }

            events.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getRecentMemberEvents", e);
        }

        return ret;
    }

    public static ArrayList<ArrayList<MemberEventObject>> getAllMemberEventsPaginated(long userId) {
        ArrayList<ArrayList<MemberEventObject>> ret = new ArrayList<ArrayList<MemberEventObject>>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            boolean isEmpty = false;
            long timestamp = OffsetDateTime.now().toEpochSecond();
            int rowsReturned = 0;
            
            do {
                PreparedStatement events = conn.prepareStatement("""
                    SELECT timestamp, fk_user, action
                    FROM member_event
                    WHERE fk_user = ?
                    AND timestamp < ?
                    ORDER BY timestamp DESC
                    LIMIT 10
                    OFFSET ?;
                    """);
                events.setLong(1, userId);
                events.setLong(2, timestamp);
                events.setInt(3, rowsReturned);
                ResultSet eventsRes = events.executeQuery();
                ArrayList<MemberEventObject> memberEvents = new ArrayList<MemberEventObject>();

                while (eventsRes.next()) {
                    MemberEventObject event = new MemberEventObject(
                        eventsRes.getLong("timestamp"), 
                        eventsRes.getLong("fk_user"), 
                        MemberEventObject.Action.valueOf(eventsRes.getString("action").toUpperCase())
                    );
                    memberEvents.add(event);
                }

                events.close();
                rowsReturned += memberEvents.size();

                if (!memberEvents.isEmpty()) {
                    ret.add(memberEvents);
                } else {
                    isEmpty = true;
                }
            } while (!isEmpty);
        } catch (SQLException e) {
            Messaging.logException("Database", "getAllMemberEventsPaginated", e);
        }

        return ret;
    }

    public static ArrayList<MemberChartData> getMemberEventsBetween(long startTimestamp, long endTimestamp, String timeUnit) {
        ArrayList<MemberChartData> ret = new ArrayList<MemberChartData>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();
        String formatStr = TimeUtils.getSQLFormatStringFromTimeUnit(timeUnit);

        try {
            PreparedStatement events = conn.prepareStatement("""
                    SELECT COUNT(timestamp) AS events, STRFTIME(?, DATETIME(timestamp, 'unixepoch')) AS timeUnit, action
                    FROM member_event
                    WHERE timestamp >= ?
                    AND timestamp <= ?
                    GROUP BY STRFTIME(?, DATETIME(timestamp, 'unixepoch')), action
                    ORDER BY CASE
                        WHEN action = "join" THEN 1
                        WHEN action = "leave" THEN 2
                        WHEN action = "ban" THEN 3
                        END ASC,
                        timestamp ASC;
                    """);
            events.setString(1, formatStr);
            events.setLong(2, startTimestamp);
            events.setLong(3, endTimestamp);
            events.setString(4, formatStr);
            ResultSet eventsRes = events.executeQuery();

            while (eventsRes.next()) {
                MemberChartData data = new MemberChartData();
                data.timeUnit = eventsRes.getString("timeUnit");
                data.events = eventsRes.getInt("events");
                data.action = eventsRes.getString("action");
                ret.add(data);
            }

            events.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getMemberEventsSince", e);
        }

        return ret;
    }

    public static void insertMemberRemoveEvent(GuildMemberRemoveEvent event, OffsetDateTime time) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {            
            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, event.getUser().getIdLong());
            insertUser.setLong(2, event.getUser().getTimeCreated().toEpochSecond());
            insertUser.setString(3, event.getUser().getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertEvent = conn.prepareStatement("""
                    INSERT INTO member_event (timestamp, fk_user, action)
                    VALUES (?, ?, ?);
                    """);
            insertEvent.setLong(1, time.toEpochSecond());
            insertEvent.setLong(2, event.getUser().getIdLong());
            insertEvent.setString(3, "leave");
            insertEvent.executeUpdate();
            insertEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "insertMemberRemoveEvent", e);
        }
    }

    public static void insertMemberBanEvent(GuildBanEvent event, OffsetDateTime time) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?) ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, event.getUser().getIdLong());
            insertUser.setLong(2, event.getUser().getTimeCreated().toEpochSecond());
            insertUser.setString(3, event.getUser().getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertEvent = conn.prepareStatement("""
                    INSERT INTO member_event (timestamp, fk_user, action)
                    VALUES (?, ?, ?);
                    """);
            insertEvent.setLong(1, time.toEpochSecond());
            insertEvent.setLong(2, event.getUser().getIdLong());
            insertEvent.setString(3, "ban");
            insertEvent.executeUpdate();
            insertEvent.close();
        } catch (SQLException e) {
             Messaging.logException("Database", "insertMemberBanEvent", e);
        }
    }

    /**
     * Insert an automod event to the database.
     * @param automodEvent
     */
    public static void insertAutoModEvent(AutoModExecutionEvent event, OffsetDateTime time) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First, try to fetch the user who triggered the event.
            Optional<User> userOpt = UserUtils.getOrRetrieveUser(event.getUserIdLong());

            // If we found them...
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Log them first
                PreparedStatement insertUser = conn.prepareStatement("""
                        INSERT INTO user (discord_id, created_datetime, username)
                        VALUES (?, ?, ?)
                        ON CONFLICT (discord_id) DO NOTHING;
                        """);
                insertUser.setLong(1, user.getIdLong());
                insertUser.setLong(2, user.getTimeCreated().toEpochSecond());
                insertUser.setString(3, user.getName());
                insertUser.executeUpdate();
                insertUser.close();

                // Then use that to also log the triggering message
                PreparedStatement insertMessage = conn.prepareStatement("""
                        INSERT INTO message (message_id, fk_channel, fk_user)
                        VALUES (?, ?, ?)
                        ON CONFLICT (message_id) DO NOTHING;
                        """);
                insertMessage.setLong(1, event.getMessageIdLong());
                insertMessage.setLong(2, event.getChannel().getIdLong());
                insertMessage.setLong(3, user.getIdLong());
                insertMessage.executeUpdate();
                insertMessage.close();
            }

            PreparedStatement insertAutoModEvent = conn.prepareStatement("""
                    INSERT INTO automod_event (fk_user, fk_message, fk_channel, alert_message_id, rule_id, timestamp, trigger, content, matched_content, matched_keyword, response_type)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                    """);

            insertAutoModEvent.setLong(1, event.getUserIdLong());
            
            if (event.getMessageIdLong() != 0) {
                insertAutoModEvent.setLong(2, event.getMessageIdLong());
            } else {
                insertAutoModEvent.setNull(2, Types.BIGINT);
            }
            
            if (event.getChannel() != null) {
                insertAutoModEvent.setLong(3, event.getChannel().getIdLong());
            } else {
                insertAutoModEvent.setNull(3, Types.BIGINT);
            }
            
            if (event.getAlertMessageIdLong() != 0) {
                insertAutoModEvent.setLong(4, event.getAlertMessageIdLong());
            } else {
                insertAutoModEvent.setNull(4, Types.BIGINT);
            }

            insertAutoModEvent.setLong(5, event.getRuleIdLong());
            insertAutoModEvent.setLong(6, time.toEpochSecond());
            insertAutoModEvent.setString(7, event.getTriggerType().toString());
            insertAutoModEvent.setString(8, event.getContent());
            insertAutoModEvent.setString(9, event.getMatchedContent());
            insertAutoModEvent.setString(10, event.getMatchedKeyword());
            insertAutoModEvent.setString(11, event.getResponse().getType().toString());
            insertAutoModEvent.executeUpdate();
            insertAutoModEvent.close();
        } catch (SQLException e) {
             Messaging.logException("Database", "insertAutoModEvent", e);
        }
    }

    public static ArrayList<AutoModEventObject> getAutoModEventsSinceTime(long userIdLong, OffsetDateTime time) {
        ArrayList<AutoModEventObject> ret = new ArrayList<AutoModEventObject>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getFilterEvent = conn.prepareStatement("""
                    SELECT
                    fk_user, fk_message, fk_channel, alert_message_id, rule_id, timestamp, trigger, content, matched_content, matched_keyword, response_type
                    FROM automod_event
                    WHERE fk_user = ?
                    AND timestamp >= ?
                    AND response_type = ?
                    ORDER BY timestamp DESC;
                    """);
            getFilterEvent.setLong(1, userIdLong);
            getFilterEvent.setLong(2, time.toEpochSecond());
            getFilterEvent.setString(3, AutoModResponse.Type.BLOCK_MESSAGE.toString());
            ResultSet res = getFilterEvent.executeQuery();

            while (res.next()) {
                AutoModEventObject autoModEventObject = new AutoModEventObject(
                    res.getLong("fk_user"),
                    res.getLong("fk_message"),
                    res.getLong("fk_channel"),
                    res.getLong("alert_message_id"),
                    res.getLong("rule_id"),
                    res.getLong("timestamp"),
                    res.getString("trigger"),
                    res.getString("content"),
                    res.getString("matched_content"),
                    res.getString("matched_keyword"),
                    res.getString("response_type")
                );

                ret.add(autoModEventObject);
            }

            getFilterEvent.close();
            return ret;
        } catch (SQLException e) {
            Messaging.logException("Database", "getAutoModEventsSinceTime", e);
        }
        
        return ret;
    }

    public static ArrayList<AutoModEventObject> getAllAutoModEvents(long userIdLong) {
        ArrayList<AutoModEventObject> ret = new ArrayList<AutoModEventObject>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getFilterEvent = conn.prepareStatement("""
                    SELECT
                    fk_user, fk_message, fk_channel, alert_message_id, rule_id, timestamp, trigger, content, matched_content, matched_keyword, response_type
                    FROM automod_event
                    WHERE fk_user = ?
                    AND (
                        response_type = ?
                        OR response_type = ?
                    )
                    ORDER BY timestamp DESC;
                    """);
            getFilterEvent.setLong(1, userIdLong);
            getFilterEvent.setString(2, AutoModResponse.Type.BLOCK_MESSAGE.toString());
            getFilterEvent.setString(3, AutoModResponse.Type.BLOCK_MEMBER_INTERACTION.toString());
            ResultSet res = getFilterEvent.executeQuery();

            while (res.next()) {
                AutoModEventObject autoModEventObject = new AutoModEventObject(
                    res.getLong("fk_user"),
                    res.getLong("fk_message"),
                    res.getLong("fk_channel"),
                    res.getLong("alert_message_id"),
                    res.getLong("rule_id"),
                    res.getLong("timestamp"),
                    res.getString("trigger"),
                    res.getString("content"),
                    res.getString("matched_content"),
                    res.getString("matched_keyword"),
                    res.getString("response_type")
                );

                ret.add(autoModEventObject);
            }

            getFilterEvent.close();
            return ret;
        } catch (SQLException e) {
            Messaging.logException("Database", "getAllAutoModEvents", e);
        }
        
        return ret;
    }

    public static ArrayList<ArrayList<AutoModEventObject>> getAllAutoModEventsPaginated(long userIdLong) {
        ArrayList<ArrayList<AutoModEventObject>> ret = new ArrayList<ArrayList<AutoModEventObject>>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            boolean isEmpty = false;
            long timestamp = OffsetDateTime.now().toEpochSecond();
            int rowsReturned = 0;

            do { 
                PreparedStatement getAutoModEvents = conn.prepareStatement("""
                        SELECT
                        fk_user, fk_message, fk_channel, alert_message_id, rule_id, timestamp, trigger, content, matched_content, matched_keyword, response_type
                        FROM automod_event
                        WHERE fk_user = ?
                        AND (
                            response_type = ?
                            OR response_type = ?
                        )
                        AND timestamp < ?
                        ORDER BY timestamp DESC
                        LIMIT 10
                        OFFSET ?;
                        """);
                getAutoModEvents.setLong(1, userIdLong);
                getAutoModEvents.setString(2, AutoModResponse.Type.BLOCK_MESSAGE.toString());
                getAutoModEvents.setString(3, AutoModResponse.Type.BLOCK_MEMBER_INTERACTION.toString());
                getAutoModEvents.setLong(4, timestamp);
                getAutoModEvents.setInt(5, rowsReturned);
                ResultSet res = getAutoModEvents.executeQuery();
                ArrayList<AutoModEventObject> autoModEvents = new ArrayList<AutoModEventObject>();

                while (res.next()) {
                    AutoModEventObject autoModEventObject = new AutoModEventObject(
                        res.getLong("fk_user"),
                        res.getLong("fk_message"),
                        res.getLong("fk_channel"),
                        res.getLong("alert_message_id"),
                        res.getLong("rule_id"),
                        res.getLong("timestamp"),
                        res.getString("trigger"),
                        res.getString("content"),
                        res.getString("matched_content"),
                        res.getString("matched_keyword"),
                        res.getString("response_type")
                    );

                    autoModEvents.add(autoModEventObject);
                }

                getAutoModEvents.close();
                rowsReturned += autoModEvents.size();

                if (!autoModEvents.isEmpty()) {
                    ret.add(autoModEvents);
                } else {
                    isEmpty = true;
                }
            } while (!isEmpty);
        } catch (SQLException e) {
            Messaging.logException("Database", "getAllAutoModEventsPaginated", e);
        }
        
        return ret;
    }

    public static void insertCounter(String type, long timestamp, long value) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement insertCounter = conn.prepareStatement("""
                    INSERT INTO counter (type, timestamp, value)
                    VALUES (?, ?, ?);
                    """);

            insertCounter.setString(1, type);
            insertCounter.setLong(2, timestamp);
            insertCounter.setLong(3, value);
            insertCounter.executeUpdate();
            insertCounter.close();
        } catch (SQLException e) {
             Messaging.logException("Database", "insertCounter", e);
        }
    }

    public static CounterObject getLatestCounter(String type) {
        CounterObject ret = null;
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getCounter = conn.prepareStatement("""
                    SELECT
                    type, timestamp, value
                    FROM counter
                    WHERE type = ?
                    ORDER BY timestamp DESC
                    LIMIT 1;
                    """);
            getCounter.setString(1, type);
            ResultSet res = getCounter.executeQuery();

            if (res.next()) {
                ret = new CounterObject(
                    res.getString("type"),
                    res.getLong("timestamp"),
                    res.getLong("value")
                );
            }

            getCounter.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getLatestCounter", e);
        }
        
        return ret;
    }

    public static void insertCommandEvent(long commandIdLong, String type, String name, String group, String sub, long eventIdLong, User user, long channelIdLong, long timestamp, boolean ninja, List<OptionMapping> options) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, user.getIdLong());
            insertUser.setLong(2, user.getTimeCreated().toEpochSecond());
            insertUser.setString(3, user.getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertCommand = conn.prepareStatement("""
                    INSERT INTO command (discord_id, type, name, subgroup, subcmd)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);

            insertCommand.setLong(1, commandIdLong);
            insertCommand.setString(2, type);
            insertCommand.setString(3, name);
            insertCommand.setString(4, group);
            insertCommand.setString(5, sub);
            insertCommand.executeUpdate();
            insertCommand.close();

            PreparedStatement insertCommandEvent = conn.prepareStatement("""
                    INSERT INTO command_event (discord_id, command_fk, user_fk, channel_fk, timestamp, ninja)
                    VALUES (?, ?, ?, ?, ?, ?);
                    """);

            insertCommandEvent.setLong(1, eventIdLong);
            insertCommandEvent.setLong(2, commandIdLong);
            insertCommandEvent.setLong(3, user.getIdLong());
            insertCommandEvent.setLong(4, channelIdLong);
            insertCommandEvent.setLong(5, timestamp);
            insertCommandEvent.setBoolean(6, ninja);
            insertCommandEvent.executeUpdate();
            insertCommandEvent.close();

            if (options.isEmpty()) {
                return;
            }

            StringBuilder sb = new StringBuilder("""
                    INSERT INTO command_event_option (command_event_fk, name, value_str)
                    VALUES
            """);

            for (int i = 0; i < options.size(); i++) {
                sb.append(" (?, ?, ?)");

                if (i < options.size() - 1) {
                    sb.append(",");
                }
            }

            sb.append(";");

            PreparedStatement insertOptions = conn.prepareStatement(sb.toString());
            int counter = 1;

            for (OptionMapping opt : options) {
                insertOptions.setLong(counter++, commandIdLong);
                insertOptions.setString(counter++, opt.getName());
                insertOptions.setString(counter++, opt.getAsString());
            }
            
            insertOptions.executeUpdate();
            insertOptions.close();
        } catch (SQLException e) {
             Messaging.logException("Database", "insertCommandEvent", e);
        }
    }

    /**
     * Get the latest command event to occur in a channel, for a given command,
     * where the user is ANYONE EXCEPT the user specified, within the ninja interval from config.
     * @param channelIdLong
     * @param commandIdLong
     * @param userIdLong
     * @return
     */
    public static Optional<CommandEventObject> getLatestCommandEventNotFromUser(long channelIdLong, long commandIdLong, long userIdLong) {
        Optional<CommandEventObject> ret = Optional.empty();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getCommandEvent = conn.prepareStatement("""
                    SELECT e.discord_id, e.command_fk, e.user_fk, e.timestamp
                    FROM command_event AS e
                    INNER JOIN command AS c ON c.discord_id = e.command_fk
                    WHERE e.channel_fk = ?
                    AND e.command_fk = ?
                    AND NOT e.user_fk = ?
                    AND e.timestamp >= ?
                    ORDER BY timestamp DESC
                    LIMIT 1;
                    """);
            getCommandEvent.setLong(1, channelIdLong);
            getCommandEvent.setLong(2, commandIdLong);
            getCommandEvent.setLong(3, userIdLong);
            OffsetDateTime now = OffsetDateTime.now().minusSeconds(HifumiBot.getSelf().getConfig().ninjaInterval);
            getCommandEvent.setLong(4, now.toEpochSecond());
            ResultSet res = getCommandEvent.executeQuery();

            if (res.next()) {
                CommandEventObject obj = new CommandEventObject(
                    res.getLong("discord_id"),
                    res.getLong("command_fk"),
                    res.getLong("user_fk"),
                    res.getLong("timestamp")
                );
                
                ret = Optional.of(obj);
            }

            getCommandEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getLatestCommandEventByChannel", e);
        }
        
        return ret;
    }

    public static ArrayList<AutomodChartData> getAutomodEventsBetween(long startTimestamp, long endTimestamp, String timeUnit) {
        ArrayList<AutomodChartData> ret = new ArrayList<AutomodChartData>();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();
        String formatStr = TimeUtils.getSQLFormatStringFromTimeUnit(timeUnit);
        
        try {
            PreparedStatement getAutomodEvents = conn.prepareStatement("""
                    SELECT COUNT(timestamp) AS events, STRFTIME(?, DATETIME(timestamp, 'unixepoch')) AS timeUnit, trigger
                    FROM automod_event
                    WHERE timestamp >= ?
                    AND timestamp <= ?
                    GROUP BY STRFTIME(?, DATETIME(timestamp, 'unixepoch')), trigger
                    ORDER BY timestamp ASC, trigger ASC;
                    """);
            getAutomodEvents.setString(1, formatStr);
            getAutomodEvents.setLong(2, startTimestamp);
            getAutomodEvents.setLong(3, endTimestamp);
            getAutomodEvents.setString(4, formatStr);
            ResultSet latestEvent = getAutomodEvents.executeQuery();

            while (latestEvent.next()) {
                AutomodChartData data = new AutomodChartData();
                data.timeUnit = latestEvent.getString("timeUnit");
                data.events = latestEvent.getInt("events");
                data.trigger = latestEvent.getString("trigger");
                ret.add(data);
            }

            getAutomodEvents.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getAutomodEventsBetween", e);
        }
        
        return ret;
    }

    public static void insertUsernameChangeEvent(UserUpdateNameEvent event) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, event.getUser().getIdLong());
            insertUser.setLong(2, event.getUser().getTimeCreated().toEpochSecond());
            insertUser.setString(3, event.getUser().getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertUsernameEvent = conn.prepareStatement("""
                    INSERT INTO user_username_event (fk_user, old_username, new_username)
                    VALUES (?, ?, ?);
                    """);
            insertUsernameEvent.setLong(1, event.getUser().getIdLong());
            insertUsernameEvent.setString(2, event.getOldName());
            insertUsernameEvent.setString(3, event.getNewName());
            insertUsernameEvent.executeUpdate();
            insertUsernameEvent.close();
        } catch (SQLException e) {
             Messaging.logException("Database", "insertUsernameChangeEvent", e);
        }
    }

    public static void insertDisplayNameChangeEvent(UserUpdateGlobalNameEvent event) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement insertUser = conn.prepareStatement("""
                    INSERT INTO user (discord_id, created_datetime, username)
                    VALUES (?, ?, ?)
                    ON CONFLICT (discord_id) DO NOTHING;
                    """);
            insertUser.setLong(1, event.getUser().getIdLong());
            insertUser.setLong(2, event.getUser().getTimeCreated().toEpochSecond());
            insertUser.setString(3, event.getUser().getName());
            insertUser.executeUpdate();
            insertUser.close();

            PreparedStatement insertDisplayNameEvent = conn.prepareStatement("""
                    INSERT INTO user_displayname_event (fk_user, old_displayname, new_displayname)
                    VALUES (?, ?, ?);
                    """);
            insertDisplayNameEvent.setLong(1, event.getUser().getIdLong());
            insertDisplayNameEvent.setString(2, event.getOldGlobalName());
            insertDisplayNameEvent.setString(3, event.getNewGlobalName());
            insertDisplayNameEvent.executeUpdate();
            insertDisplayNameEvent.close();
        } catch (SQLException e) {
             Messaging.logException("Database", "insertDisplayNameChangeEvent", e);
        }
    }

    public static void insertInteractionEvent(long eventId, long timestamp, long userId) {
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            PreparedStatement insertInteractionEvent = conn.prepareStatement("""
                    INSERT INTO interaction_event (id, timestamp, user_fk)
                    VALUES (?, ?, ?);
                    """);
            insertInteractionEvent.setLong(1, eventId);
            insertInteractionEvent.setLong(2, timestamp);
            insertInteractionEvent.setLong(3, userId);
            insertInteractionEvent.executeUpdate();
            insertInteractionEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "insertInteractionEvent", e);
        }   
    }

    public static Optional<InteractionEventObject> getInteractionEvent(long eventId, long userId) {
        Optional<InteractionEventObject> ret = Optional.empty();
        Connection conn = HifumiBot.getSelf().getSQLite().getConnection();

        try {
            // First get the latest revision of the message
            PreparedStatement getInteractionEvent = conn.prepareStatement("""
                    SELECT id, timestamp, user_fk
                    FROM interaction_event
                    WHERE id = ?
                    AND user_fk = ?
                    ORDER BY timestamp DESC
                    LIMIT 1;
                    """);
            getInteractionEvent.setLong(1, eventId);
            getInteractionEvent.setLong(2, userId);
            ResultSet res = getInteractionEvent.executeQuery();

            if (res.next()) {
                ret = Optional.of(new InteractionEventObject(
                    res.getLong("id"),
                    res.getLong("timestamp"),
                    res.getLong("user_fk")
                ));
            }

            getInteractionEvent.close();
        } catch (SQLException e) {
            Messaging.logException("Database", "getInteractionEvent", e);
        }
        
        return ret;
    }
}
