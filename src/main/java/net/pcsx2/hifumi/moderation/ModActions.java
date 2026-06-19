// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.moderation;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.util.Log;
import net.pcsx2.hifumi.util.Messaging;

public class ModActions {

    private static final String BOT_FOOTER = "Don't know why you are receiving this message? Please check that your Discord account is secure, someone might be using your account as a spam bot.";

    public static void deleteMessagesMatchingSince(Message message, long timestamp) {
        ArrayList<MessageObject> duplicates = Database.getIdenticalMessagesSinceTime(message.getAuthor().getIdLong(), message.getContentRaw(), timestamp);

        for (MessageObject duplicate : duplicates) {
            try {
                HifumiBot.getSelf().getJDA().getTextChannelById(duplicate.getChannelId()).deleteMessageById(duplicate.getMessageId()).queue();
            } catch (Exception e) {
                // Squelch
            }
        }
    }
    
    public static void deleteAllMessageFromUserSince(long userIdLong, long timestamp) {
        ArrayList<MessageObject> messageList = Database.getAllMessagesSinceTime(userIdLong, timestamp);

        for (MessageObject message : messageList) {
            GuildChannel channel = HifumiBot.getSelf().getJDA().getGuildChannelById(message.getChannelId());
            
            if (channel != null && channel instanceof GuildMessageChannel) {
                GuildMessageChannel mChannel = (GuildMessageChannel) channel;
                mChannel.deleteMessageById(message.getMessageId()).queue();
            }
        }
    }
    
    public static void deleteAllMessageFromUserSinceExcept(long userIdLong, long timestamp, long exceptedMessageId) {
        ArrayList<MessageObject> messageList = Database.getAllMessagesSinceTimeExcept(userIdLong, timestamp, exceptedMessageId);

        for (MessageObject message : messageList) {
            GuildChannel channel = HifumiBot.getSelf().getJDA().getGuildChannelById(message.getChannelId());
            
            if (channel != null && channel instanceof GuildMessageChannel) {
                GuildMessageChannel mChannel = (GuildMessageChannel) channel;
                mChannel.deleteMessageById(message.getMessageId()).queue();
            }
        }
    }
    
    public static boolean timeoutAndNotifyUser(Guild server, String userId) {
        return timeoutAndNotifyUser(server, Long.valueOf(userId));
    }

    public static boolean timeoutAndNotifyUser(Guild server, long userIdLong) {
        try {
            Member member = server.retrieveMemberById(userIdLong).complete();

            if (member != null) {
                if (!member.isTimedOut()) {
                    member.timeoutFor(Duration.ofMinutes(HifumiBot.getSelf().getConfig().modActionOptions.timeoutDurationMinutes)).complete();

                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Timed Out in PCSX2 Server");
                    eb.setDescription(HifumiBot.getSelf().getConfig().modActionOptions.timeoutMessage);
                    eb.setFooter(BOT_FOOTER);
                    eb.setColor(Color.RED);
                    Messaging.sendPrivateMessageEmbed(member.getUser(), eb.build());
                    return true;
                }
            }
        } catch (InsufficientPermissionException e) {
            Messaging.logInfo("ModActions", "timeoutAndNotifyUser", "Someone just tried to take a timeout action against user ID " + userIdLong + ", but that user cannot be timed out due to elevated permissions.");
        } catch (Exception e) {
            Messaging.logException("ModActions", "timeoutAndNotifyUser", e);
        }
        
        return false;
    }
    
    public static boolean kickAndNotifyUser(Guild server, String userId) {
        return kickAndNotifyUser(server, Long.valueOf(userId));
    }

    public static boolean kickAndNotifyUser(Guild server, long userIdLong) {
        Log.info("Kick and notify action start");

        try {
            Member member = server.retrieveMemberById(userIdLong).complete();
            User memberAsUser = member.getUser();
            Log.info("Kick and notify member retrieved");

            if (member != null) {
                Log.info("Kick and notify embed building");
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Kicked From PCSX2 Server");
                eb.setDescription(HifumiBot.getSelf().getConfig().modActionOptions.kickMessage);
                eb.setFooter(BOT_FOOTER);
                eb.setColor(Color.RED);
                Log.info("Kick and notify sending dm");
                Messaging.sendPrivateMessageEmbed(member.getUser(), eb.build());
                Log.info("Kick and notify kicking user");
                member.ban(60, TimeUnit.MINUTES).complete();
                server.unban(memberAsUser).complete();
                Log.info("Kick and notify returning true");
                return true;
            }
        } catch (Exception e) {
            Log.error(e);
        }
        
        Log.info("Kick and notify returning false");
        return false;
    }
}
