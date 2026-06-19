// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.permissions;

import java.util.ArrayList;
import java.util.Optional;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.util.MemberUtils;

public class PermissionManager {
    
    private String superuserId;

    public PermissionManager(String superuserId) {
        this.superuserId = superuserId;
        
        if (HifumiBot.getSelf().getConfig().permissions.blockedRoleIds == null) {
            HifumiBot.getSelf().getConfig().permissions.blockedRoleIds = new ArrayList<String>();
        }
        
        if (HifumiBot.getSelf().getConfig().permissions.modRoleIds == null) {
            HifumiBot.getSelf().getConfig().permissions.modRoleIds = new ArrayList<String>();
        }
        
        if (HifumiBot.getSelf().getConfig().permissions.adminRoleIds == null) {
            HifumiBot.getSelf().getConfig().permissions.adminRoleIds = new ArrayList<String>();
        }
        
        if (HifumiBot.getSelf().getConfig().permissions.superAdminRoleIds == null) {
            HifumiBot.getSelf().getConfig().permissions.superAdminRoleIds = new ArrayList<String>();
        }
        
        this.validateRoles();
    }

    public boolean hasPermission(PermissionLevel permissionLevel, Member member) {
        if (member == null) {
            return false;
        }
        
        switch (permissionLevel) {
        case GUEST:
            for (Role role : member.getRoles()) {
                if (HifumiBot.getSelf().getConfig().permissions.blockedRoleIds.contains(role.getId())) {
                    return false;
                }
            }
            
            return true;
        case MOD:
            for (Role role : member.getRoles()) {
                if (HifumiBot.getSelf().getConfig().permissions.modRoleIds.contains(role.getId())) {
                    return true;
                }
            }
        case ADMIN:
            for (Role role : member.getRoles()) {
                if (HifumiBot.getSelf().getConfig().permissions.adminRoleIds.contains(role.getId())) {
                    return true;
                }
            }
        case SUPER_ADMIN:
            for (Role role : member.getRoles()) {
                if (HifumiBot.getSelf().getConfig().permissions.superAdminRoleIds.contains(role.getId())) {
                    return true;
                }
            }
        case SUPERUSER:
            if (superuserId != null && superuserId.equals(member.getUser().getId())) {
                return true;
            }
        default:
            return false;
        }
    }
    
    public boolean hasPermission(PermissionRole permissionRole, Member member) {
        if (permissionRole != null && member != null) {
            for (Role role : member.getRoles()) {
                if (role.getName().equals(permissionRole.toString())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Convenience method to simplify checking if a message's author has the log bypass permission.
     * @param message - The message to check
     * @return True if the author has the bypass permission, false otherwise.
     */
    public boolean hasMessageLogBypass(Message message) {
        if (message.getChannelType().isGuild()) {
            Guild server = message.getGuild();
            Optional<Member> memberOpt = MemberUtils.getOrRetrieveMember(server, message.getAuthor().getId());
            
            if (memberOpt.isPresent()) {
                return this.hasPermission(PermissionRole.MESSAGE_LOG_BYPASS_ROLE, memberOpt.get());
            }
        }
        
        return false;
    }
    
    private void validateRoles(String... roleNames) {
        for (Guild server : HifumiBot.getSelf().getJDA().getGuilds()) {
            for (PermissionRole permissionRole : PermissionRole.values()) {
                String roleName = permissionRole.toString();
                
                if (server.getRolesByName(roleName, true).isEmpty()) {
                    server.createRole()
                        .setName(roleName)
                        .setMentionable(false)
                        .queue();
                }
            }
        }
    }
}
