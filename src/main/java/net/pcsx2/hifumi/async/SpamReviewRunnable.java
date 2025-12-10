package net.pcsx2.hifumi.async;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

public class SpamReviewRunnable implements Runnable {

    private final Message message;
    private final OffsetDateTime time;

    public SpamReviewRunnable(Message message, OffsetDateTime time) {
        this.message = message;
        this.time = time;
    }

    @Override
    public void run() {
        if (this.reviewSpam()) {
            User usr = this.message.getAuthor();

            // Timeout the user first
            ModActions.timeoutAndNotifyUser(this.message.getGuild(), usr.getIdLong());

            // Now delete any offending messages
            long cooldownSeconds = HifumiBot.getSelf().getConfig().spamOptions.cooldownSeconds;
            OffsetDateTime cooldownSubtracted = this.time.minusSeconds(cooldownSeconds);
            long cooldownEpochSeconds = cooldownSubtracted.toEpochSecond();
            ModActions.deleteMessagesMatchingSince(this.message, cooldownEpochSeconds);
            
            // Now report to staff
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("User Automatically Timed Out");
            eb.setDescription("User was automatically timed out for spam.");
            eb.addField("User (As Mention)", usr.getAsMention(), true);
            eb.addField("Username", usr.getName(), true);
            eb.addField("User ID", usr.getId(), true);
            eb.setColor(Color.CYAN);
            Messaging.logInfoEmbed(eb.build());

            // Finally, tell them why
            eb = new EmbedBuilder();
            eb.setTitle("Spam Warning");
            eb.setDescription(HifumiBot.getSelf().getConfig().spamOptions.message);
            eb.setColor(Color.YELLOW);
            Messaging.sendPrivateMessageEmbed(usr, eb.build());
        }
    }

    private boolean reviewSpam() {
        String rawContent = this.message.getContentRaw();

        if (rawContent == null || rawContent.isEmpty()) {
            return false;
        }

        long cooldownSeconds = HifumiBot.getSelf().getConfig().autoModOptions.cooldownSeconds;
        OffsetDateTime cooldownSubtracted = this.time.minusSeconds(cooldownSeconds);
        long cooldownEpochSeconds = cooldownSubtracted.toEpochSecond();
        ArrayList<MessageObject> duplicates = Database.getIdenticalMessagesSinceTime(this.message.getAuthor().getIdLong(), this.message.getContentRaw(), cooldownEpochSeconds);

        if (duplicates == null || duplicates.isEmpty()) {
            return false;
        }

        if (duplicates.size() >= HifumiBot.getSelf().getConfig().spamOptions.maxMessages) {
            return true;
        }

        return false;
    }
}
