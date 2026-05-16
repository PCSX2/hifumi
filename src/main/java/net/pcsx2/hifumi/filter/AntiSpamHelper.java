package net.pcsx2.hifumi.filter;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.AttachmentUtils;
import net.pcsx2.hifumi.util.Messaging;

public class AntiSpamHelper implements IFilterHelper {

    private final Message message;

    public AntiSpamHelper(Message message) {
        this.message = message;
    }

    @Override
    public boolean run() {
        boolean isSpam = this.reviewSpam();
        
        if (isSpam) {
            User usr = this.message.getAuthor();

            // Timeout the user first
            boolean timeoutRes = ModActions.timeoutAndNotifyUser(this.message.getGuild(), usr.getIdLong());
            
            long cooldownSeconds = HifumiBot.getSelf().getConfig().spamOptions.cooldownSeconds;
            OffsetDateTime cooldownSubtracted = this.message.getTimeCreated().minusSeconds(cooldownSeconds);
            long cooldownEpochSeconds = cooldownSubtracted.toEpochSecond();
            ModActions.deleteMessagesMatchingSince(this.message, cooldownEpochSeconds);

            if (timeoutRes) {
                ArrayList<FileUpload> files = AttachmentUtils.getMinifiedAttachments(message);
                
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("User Automatically Timed Out");
                eb.setDescription("User was automatically timed out for spam.");
                eb.addField("User (As Mention)", usr.getAsMention(), true);
                eb.addField("Username", usr.getName(), true);
                eb.addField("User ID", usr.getId(), true);
                eb.setColor(Color.YELLOW);
                
                MessageCreateBuilder mb = new MessageCreateBuilder();
                mb.addEmbeds(eb.build());
                mb.addFiles(files);
                mb.addComponents(ActionRow.of(
                    Button.of(ButtonStyle.DANGER, "imagescam:dospamkick:" + usr.getIdLong(), "Looks like a bot scam, kick user"), 
                    Button.of(ButtonStyle.SUCCESS, "imagescam:clear:" + usr.getIdLong(), "Looks innocent, remove timeout")
                ));
                
                Messaging.sendMessage(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, mb.build());
            } else {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Failed to timeout spammer");
                eb.setDescription("Detected a spammer, but was unable to issue a timeout command. Please check if the suspected spammer is still in the server and if any further action is required.");
                eb.addField("User (As Mention)", usr.getAsMention(), true);
                eb.addField("Username", usr.getName(), true);
                eb.addField("User ID", usr.getId(), true);
                eb.setColor(Color.RED);
                Messaging.logInfoEmbed(eb.build());
            }
            
            EmbedBuilder eb = new EmbedBuilder();
            eb = new EmbedBuilder();
            eb.setTitle("Spam Warning");
            eb.setDescription(HifumiBot.getSelf().getConfig().spamOptions.message);
            eb.setColor(Color.YELLOW);
            Messaging.sendPrivateMessageEmbed(usr, eb.build());
        }
        
        return isSpam;
    }

    private boolean reviewSpam() {
        long cooldownSeconds = HifumiBot.getSelf().getConfig().spamOptions.cooldownSeconds;
        OffsetDateTime cooldownSubtracted = this.message.getTimeCreated().minusSeconds(cooldownSeconds);
        long cooldownEpochSeconds = cooldownSubtracted.toEpochSecond();
        HashMap<Long, Integer> aggregateResults = Database.getMessageAggregateCountsByChannelSinceTime(this.message.getAuthor().getIdLong(), cooldownEpochSeconds);

        if (aggregateResults.size() >= HifumiBot.getSelf().getConfig().spamOptions.maxMessages) {
            return true;
        }

        return false;
    }
}
