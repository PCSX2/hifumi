/**
 * This file is part of HifumiBot, licensed under the MIT License (MIT)
 * 
 * Copyright (c) 2020 RedPanda4552 (https://github.com/RedPanda4552)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package net.pcsx2.hifumi.permissions;

import java.util.ArrayList;

import net.pcsx2.hifumi.HifumiBot;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

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
}
