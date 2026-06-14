package net.pcsx2.hifumi.filter;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.AttachmentUtils;
import net.pcsx2.hifumi.util.EmbedUtil;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.RoleUtils;

public class HoneypotHelper implements IFilterHelper {

    private static final int AGE_MINUTES_TO_REMOVE_MESSAGES = 5;
    
    private final Message message;
    
    public HoneypotHelper(Message message) {
        this.message = message;
    }
    
    @Override
    public boolean run() {
        String honeypotChannelId = HifumiBot.getSelf().getConfig().honeypotOptions.channelId;
        String honeypotRoleId = HifumiBot.getSelf().getConfig().honeypotOptions.roleId;
        Guild server = this.message.getGuild();
        Member member = this.message.getMember();
        
        // First of all, did they post in the honeypot        
        if (honeypotChannelId != null && this.message.getChannelId().equals(honeypotChannelId)) {
            // Check some elevated permissions for people we might want to exempt.
            if (member.hasPermission(Permission.MANAGE_SERVER, Permission.MANAGE_ROLES, Permission.MESSAGE_MANAGE)) {
                return false;
            }
            
            // Now actually do the role assignment
            if (honeypotRoleId != null) {
                if (RoleUtils.memberHasRole(member, honeypotRoleId)) {
                    return true;
                }
                
                Role honeypotRole = server.getRoleById(honeypotRoleId);
                // Block so we can be sure the event handler will be ready to sweep up new messages
                // and the below delete will not miss any in between actions 
                server.addRoleToMember(member, honeypotRole).complete();
                
                OffsetDateTime currentTime = OffsetDateTime.now();
                OffsetDateTime cutoffTime = currentTime.minusMinutes(AGE_MINUTES_TO_REMOVE_MESSAGES);
                ModActions.deleteAllMessageFromUserSinceExcept(member.getIdLong(), cutoffTime.toEpochSecond(), this.message.getIdLong());
                return true;
            }
        // If not the honeypot channel, but they have the role
        } else if (RoleUtils.memberHasRole(member, honeypotRoleId)) {
            // Smite them
            ModActions.kickAndNotifyUser(server, member.getIdLong());
            OffsetDateTime currentTime = OffsetDateTime.now();
            OffsetDateTime cutoffTime = currentTime.minusMinutes(AGE_MINUTES_TO_REMOVE_MESSAGES);
            ModActions.deleteAllMessageFromUserSince(member.getIdLong(), cutoffTime.toEpochSecond());
            Database.insertHoneypotEvent(currentTime.toEpochSecond(), member.getIdLong(), this.message.getIdLong());
            this.notifyStaff();
            this.updateChannel();
            return true;
        }
        
        return false;
    }
    
    private void notifyStaff() {
        User user = this.message.getAuthor();
        
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Automatically fired /spamkick on user in honeypot");
        eb.setDescription("User has fallen into the honeypot and was automatically kicked after posting a message in another channel.\n\n");
        eb.appendDescription("Their messages sent within the last " + AGE_MINUTES_TO_REMOVE_MESSAGES + " minutes should be deleted or are in the process of being deleted.\n\n");
        eb.appendDescription("A short reference of what they sent is included below. No further action is required.\n\n");
        eb.addField("User ID", user.getId(), true);
        eb.addField("Username", user.getName(), true);
        eb.addField("Display Name (as mention)", user.getAsMention(), true);
        eb.setColor(Color.YELLOW);
        
        // Body content preview
        eb.addField("Body Content (raw, first 100 chars)", StringUtils.abbreviate(this.message.getContentRaw(), 100), false);
        
        // Attachments
        eb.addField(EmbedUtil.newAttachmentListField(this.message.getAttachments()));

        ArrayList<FileUpload> files = AttachmentUtils.getMinifiedAttachments(message);
        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addEmbeds(eb.build());
        mb.addFiles(files);
        Messaging.logInfoMessage(mb.build());
    }
    
    private void updateChannel() {
        OffsetDateTime currentTime = OffsetDateTime.now();
        int year = currentTime.getYear();
        int month = currentTime.getMonthValue();
        OffsetDateTime startOfMonth = OffsetDateTime.of(year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        Optional<Integer> eventCountOpt = Database.getHoneypotEventCountSince(startOfMonth.toEpochSecond());
        
        if (eventCountOpt.isPresent()) {
            StringBuilder sb = new StringBuilder()
                    .append(HifumiBot.getSelf().getJDA().getSelfUser().getAsMention())
                    .append(" killed ")
                    .append(this.message.getAuthor().getAsMention());
            
            Integer eventCount = eventCountOpt.get();
            boolean shouldRefresh = true;
            
            switch (eventCount) {
                case 5 -> {
                    sb.append("\nKilling spree!");
                }
                case 10 -> {
                    sb.append("\nKilling frenzy!");
                }
                case 15 -> {
                    sb.append("\nRunning riot!");
                }
                case 20 -> {
                    sb.append("\nRampage!");
                }
                case 25 -> {
                    sb.append("\nUntouchable!");
                }
                case 30 -> {
                    sb.append("\nInvincible!");
                }
                case 35 -> {
                    sb.append("\nInconceivable!");
                }
                default -> {
                    shouldRefresh = false;
                }
            }
            
            MessageCreateBuilder mb = new MessageCreateBuilder();
            mb.setContent(sb.toString());
            Messaging.sendMessage(HifumiBot.getSelf().getConfig().honeypotOptions.channelId, mb.build());
            
            if (shouldRefresh) {
                mb = new MessageCreateBuilder();
                mb.setContent(HifumiBot.getSelf().getConfig().honeypotOptions.warningMessage);
                Messaging.sendMessage(HifumiBot.getSelf().getConfig().honeypotOptions.channelId, mb.build());
            }
        }
    }
}
