package net.pcsx2.hifumi.async;

import java.util.List;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.permissions.PermissionLevel;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EntryBarrierRunnable implements Runnable {

    private MessageReceivedEvent event;

    public EntryBarrierRunnable(MessageReceivedEvent event) {
        this.event = event;
    }
    
    @Override
    public void run() {
        Member member = event.getMember();

        // Right away, check permissions. If the member is some elevated role, just stop immediately.
        if (HifumiBot.getSelf().getPermissionManager().hasPermission(PermissionLevel.MOD, member)) {
            return;
        }

        // Now look to see if they already have the entry role
        List<Role> roles = member.getRoles();

        for (Role role : roles) {
            if (role.getId().equals(HifumiBot.getSelf().getConfig().entryBarrierOptions.entryRoleId)) {
                return;
            }
        }

        // Finally, check the message content to see if it passes the test.
        String messageContent = event.getMessage().getContentStripped();
        
        if (messageContent != null && messageContent.equals(HifumiBot.getSelf().getConfig().entryBarrierOptions.expectedUserInput)) {
            this.event.getMessage().delete().queue();

            Role entryRole = this.event.getGuild().getRoleById(HifumiBot.getSelf().getConfig().entryBarrierOptions.entryRoleId);
            this.event.getGuild().addRoleToMember(this.event.getMember(), entryRole).queue();
        }
    }
}
