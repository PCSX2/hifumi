package net.pcsx2.hifumi.moderation;

import java.util.HashMap;
import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.pcsx2.hifumi.HifumiBot;

public class SuspectedSpammerHandler {

    private static final String SPAMMER_ROLE = "Suspected Spam Bot";
    
    private HashMap<String, Role> roleMap;
    
    public SuspectedSpammerHandler() {
        this.roleMap = new HashMap<String, Role>();
        
        for (Guild server : HifumiBot.getSelf().getJDA().getGuilds()) {
            if (server.getRolesByName(SPAMMER_ROLE, true).isEmpty()) {
                server.createRole()
                    .setName(SPAMMER_ROLE)
                    .setMentionable(false)
                    .complete();
            }
            
            List<Role> roles = server.getRolesByName(SPAMMER_ROLE, true);
            
            if (!roles.isEmpty()) {
                roleMap.put(server.getId(), roles.get(0));
            }
        }
    }
    
    public void evaluateMember(Member member) {
        if ((member.getFlagsRaw() & 0x04000000) > 0) { // (1 << 20)
            Guild server = member.getGuild();
            Role role = roleMap.get(server.getId());
            
            for (Role existingRole : member.getRoles()) {
                if (existingRole.getIdLong() == role.getIdLong()) {
                    return;
                }
            }
            
            server.addRoleToMember(member, role).queue();
        }
    }
}
