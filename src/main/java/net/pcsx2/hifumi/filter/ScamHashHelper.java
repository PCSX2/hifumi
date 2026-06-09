package net.pcsx2.hifumi.filter;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.ScamHashObject;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.AttachmentUtils;
import net.pcsx2.hifumi.util.Messaging;

public class ScamHashHelper implements IFilterHelper {

    private static final int AGE_MINUTES_TO_REMOVE_MESSAGES = 5;
    
    private final Message message;
    
    private String sha256;
    private String hashDescription;
    
    public ScamHashHelper(Message message) {
        this.message = message;
    }
    
    @Override
    public boolean run() {
        boolean res = this.evaluate();
        
        if (res) {
            this.autoKick();
            Database.insertScamHashMatch(OffsetDateTime.now().toEpochSecond(), this.sha256, this.message.getIdLong());
            this.notifyStaff();
        }
        
        return res;
    }

    private boolean evaluate() {
        for (Attachment attachment : this.message.getAttachments()) {
            Optional<String> sha256Opt = AttachmentUtils.generateImageSHA256(attachment);
            
            if (sha256Opt.isPresent()) {
                this.sha256 = sha256Opt.get();
                Optional<ScamHashObject> scamHashOpt = Database.getActiveScamHash(this.sha256);
                
                if (scamHashOpt.isPresent()) {
                    this.hashDescription = scamHashOpt.get().getDescription();
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void autoKick() {
        Guild server = this.message.getGuild();
        Member member = this.message.getMember();
        
        ModActions.kickAndNotifyUser(server, member.getIdLong());
        OffsetDateTime currentTime = OffsetDateTime.now();
        OffsetDateTime cutoffTime = currentTime.minusMinutes(AGE_MINUTES_TO_REMOVE_MESSAGES);
        ModActions.deleteAllMessageFromUserSince(member.getIdLong(), cutoffTime.toEpochSecond());
    }
    
    private void notifyStaff() {
        User user = this.message.getAuthor();
        
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Automatically fired /spamkick for matching spam hash");
        eb.setDescription("User has posted a message attachment with a hash matching a known spam attachment.\n\n");
        eb.appendDescription("Their messages sent within the last " + AGE_MINUTES_TO_REMOVE_MESSAGES + " minutes should be deleted or are in the process of being deleted.\n\n");
        eb.appendDescription("A short reference of what they sent is included below. No further action is required.\n\n");
        eb.addField("User ID", user.getId(), true);
        eb.addField("Username", user.getName(), true);
        eb.addField("Display Name (as mention)", user.getAsMention(), true);
        eb.addField("Matched SHA3-256 Hash", this.sha256, false);
        eb.addField("Hash Description", this.hashDescription, false);
        eb.setColor(Color.YELLOW);
        
        // Body content preview
        eb.addField("Body Content (raw, first 100 chars)", StringUtils.abbreviate(this.message.getContentRaw(), 100), false);
        
        // Attachments
        StringBuilder sb = new StringBuilder();

        for (Attachment attachment : this.message.getAttachments()) {
            sb.append(attachment.getProxyUrl() + "\n");
        }
        
        eb.addField("Attachments", sb.toString(), false);

        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addEmbeds(eb.build());
        Messaging.logInfoMessage(mb.build());
    }
}
