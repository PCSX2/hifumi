package net.pcsx2.hifumi.filter;

import java.time.OffsetDateTime;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.moderation.ModActions;
import net.pcsx2.hifumi.util.RoleUtils;

public class HoneypotFilter implements IFilterHelper {

    private final Message message;
    
    public HoneypotFilter(Message message) {
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
                OffsetDateTime cutoffTime = currentTime.minusMinutes(5);
                ModActions.deleteAllMessageFromUserSinceExcept(member.getIdLong(), cutoffTime.toEpochSecond(), this.message.getIdLong());
                return true;
            }
        // If not the honeypot channel, but they have the role
        } else if (RoleUtils.memberHasRole(member, honeypotRoleId)) {
            // Smite them
            ModActions.kickAndNotifyUser(server, member.getIdLong());
            OffsetDateTime currentTime = OffsetDateTime.now();
            OffsetDateTime cutoffTime = currentTime.minusMinutes(5);
            ModActions.deleteAllMessageFromUserSince(member.getIdLong(), cutoffTime.toEpochSecond());
            return true;
        }
        
        return false;
    }
}
