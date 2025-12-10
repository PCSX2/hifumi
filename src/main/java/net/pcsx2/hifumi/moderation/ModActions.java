package net.pcsx2.hifumi.moderation;

import java.awt.Color;
import java.time.Duration;
import java.util.ArrayList;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.util.Log;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

public class ModActions {

    private static final String BOT_FOOTER = "Don't know why you are receiving this message? Please check that your Discord account is secure, someone might be using your account as a spam bot.";

    public static synchronized void deleteMessagesMatchingSince(Message message, long timestamp) {
        ArrayList<MessageObject> duplicates = Database.getIdenticalMessagesSinceTime(message.getAuthor().getIdLong(), message.getContentRaw(), timestamp);

        for (MessageObject duplicate : duplicates) {
            try {
                HifumiBot.getSelf().getJDA().getTextChannelById(duplicate.getChannelId()).deleteMessageById(duplicate.getMessageId()).queue();
            } catch (Exception e) {
                // Squelch
            }
        }
    }

    public static synchronized boolean timeoutAndNotifyUser(Guild server, long userIdLong) {
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
        } catch (Exception e) {
            Messaging.logException("FilterHandler", "timeoutUser", e);
        }
        
        return false;
    }

    public static synchronized boolean kickAndNotifyUser(Guild server, long userIdLong) {
        Log.info("Kick and notify action start");

        try {
            Member member = server.retrieveMemberById(userIdLong).complete();
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
                member.kick().complete();
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
