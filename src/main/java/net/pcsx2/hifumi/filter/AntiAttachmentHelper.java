package net.pcsx2.hifumi.filter;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;

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
import net.pcsx2.hifumi.database.objects.MessageObject;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.AttachmentUtils;
import net.pcsx2.hifumi.util.Messaging;

public class AntiAttachmentHelper implements IFilterHelper {

    private static final int AGE_MINUTES_TO_REMOVE_MESSAGES = 5;
    
    private final Message message;
    private final User user;
    
    public AntiAttachmentHelper(Message message) {
        this.message = message;
        this.user = this.message.getAuthor();
    }
    
    @Override
    public boolean run() {
        boolean isMultipleAttachments = this.isMultipleAttachments();
        
        if (isMultipleAttachments) {
            boolean timeoutRes = ModActions.timeoutAndNotifyUser(this.message.getGuild(), this.user.getIdLong());
            OffsetDateTime cooldownSubtracted = this.message.getTimeCreated().minusMinutes(AGE_MINUTES_TO_REMOVE_MESSAGES);
            ModActions.deleteAllMessageFromUserSince(this.user.getIdLong(), cooldownSubtracted.toEpochSecond());
            
            if (timeoutRes) {
                this.sendSuccessMessage();
            } else {
                this.sendFailMessage();
            }
        }
        
        return isMultipleAttachments;
    }
    
    /**
     * Test if a bot has posted multiple messages which include attachments.
     * @return True if the threshold for message count was met within the configured time, false otherwise.
     */
    private boolean isMultipleAttachments() {
        int cooldownSeconds = HifumiBot.getSelf().getConfig().antiAttachmentOptions.cooldownSeconds;
        OffsetDateTime cooldownTime = OffsetDateTime.now().minusSeconds(cooldownSeconds);
        ArrayList<MessageObject> messages = Database.getMessagesWithAttachmentsAggregateByChannelSinceTime(this.message.getAuthor().getIdLong(), cooldownTime.toEpochSecond());
        
        if (messages == null || messages.isEmpty()) {
            return false;
        }
        
        if (messages.size() >= HifumiBot.getSelf().getConfig().antiAttachmentOptions.maxMessages) {
            return true;
        }
        
        return false;
    }
    
    private void sendSuccessMessage() {
        ArrayList<FileUpload> files = AttachmentUtils.getMinifiedAttachments(message);
        
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("User Automatically Timed Out");
        eb.setDescription("User was automatically timed out for attachment spam.");
        eb.addField("User (As Mention)", this.user.getAsMention(), true);
        eb.addField("Username", this.user.getName(), true);
        eb.addField("User ID", this.user.getId(), true);
        eb.setColor(Color.YELLOW);
        
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addEmbeds(eb.build());
        mb.addFiles(files);
        mb.addComponents(ActionRow.of(
            Button.of(
                    ButtonStyle.DANGER, 
                    "spamkick:execute:" + this.user.getId() + ":" + this.message.getId() + ":spam_attachment", 
                    "Looks like a bot scam, kick user"
            ), 
            Button.of(
                    ButtonStyle.SUCCESS, 
                    "spamkick:clear:" + this.user.getId() + ":" + this.message.getId() + ":spam_attachment", 
                    "Looks innocent, remove timeout"
            )
        ));
        
        Messaging.sendMessage(HifumiBot.getSelf().getConfig().channels.systemOutputChannelId, mb.build());
    }
    
    private void sendFailMessage() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Failed to timeout spammer");
        eb.setDescription("Detected an attachment spammer, but was unable to issue a timeout command. Please check if the suspected spammer is still in the server and if any further action is required.");
        eb.addField("User (As Mention)", this.user.getAsMention(), true);
        eb.addField("Username", this.user.getName(), true);
        eb.addField("User ID", this.user.getId(), true);
        eb.setColor(Color.RED);
        Messaging.logInfoEmbed(eb.build());
    }
}
