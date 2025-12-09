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
package net.pcsx2.hifumi.command.slash;

import java.util.ArrayList;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.config.ConfigManager;
import net.pcsx2.hifumi.permissions.PermissionLevel;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class CommandPerms extends AbstractSlashCommand {
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        OptionMapping optPermissionLevel = event.getOption("permission-level");
        PermissionLevel permissionLevel = null;
        ArrayList<String> roleIds = null;
        
        if (optPermissionLevel != null) {
            permissionLevel = PermissionLevel.valueOf(optPermissionLevel.getAsString());
            
            switch (permissionLevel) {
            case BLOCKED:
                roleIds = HifumiBot.getSelf().getConfig().permissions.blockedRoleIds;
                break;
            case MOD:
                roleIds = HifumiBot.getSelf().getConfig().permissions.modRoleIds;
                break;
            case ADMIN:
                roleIds = HifumiBot.getSelf().getConfig().permissions.adminRoleIds;
                break;
            case SUPER_ADMIN:
                roleIds = HifumiBot.getSelf().getConfig().permissions.superAdminRoleIds;
                break;
            default:
                event.getHook().sendMessage("Sanity check.").queue();
                return;
            }
            
            if (roleIds == null) {
                roleIds = new ArrayList<String>();
            }
        }
        
        Role role = null;
        OptionMapping optRole = event.getOption("role");
        
        if (optRole != null) {
            role = optRole.getAsRole();
        }

        switch (event.getSubcommandName()) {
        case "add":
            if (!roleIds.contains(role.getId())) {
                roleIds.add(role.getId());
                ConfigManager.write(HifumiBot.getSelf().getConfig());
                event.getHook().sendMessage(":white_check_mark: Role " + role.getName() + " added to permission level " + permissionLevel.toString() + ".").queue();
            } else {
                event.getHook().sendMessage(":x: Role " + role.getName() + " is already a member of permission level " + permissionLevel.toString() + ".").queue();
            }
            break;
        case "del":
            if (roleIds.contains(role.getId())) {
                roleIds.remove(role.getId());
                ConfigManager.write(HifumiBot.getSelf().getConfig());
                event.getHook().sendMessage(":white_check_mark: Role " + role.getName() + " removed from permission level " + permissionLevel.toString() + ".").queue();
            } else {
                event.getHook().sendMessage(":x: Role " + role.getName() + " is not a member of permission level " + permissionLevel.toString() + ".").queue();
            }
            break;
        case "list":
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(HifumiBot.getSelf().getJDA().getSelfUser().getName() + " - Roles Assigned to Permission Levels");
            StringBuilder sb = new StringBuilder();
            
            for (String blockedRoleId : HifumiBot.getSelf().getConfig().permissions.blockedRoleIds) {
                sb.append("`" + event.getGuild().getRoleById(blockedRoleId).getName() + "`\t");
            }

            eb.addField("BLOCKED", sb.toString(), false);
            sb = new StringBuilder();

            for (String modRoleId : HifumiBot.getSelf().getConfig().permissions.modRoleIds) {
                sb.append("`" + event.getGuild().getRoleById(modRoleId).getName() + "`\t");
            }

            eb.addField("MOD", sb.toString(), false);
            sb = new StringBuilder();

            for (String adminRoleId : HifumiBot.getSelf().getConfig().permissions.adminRoleIds) {
                sb.append("`" + event.getGuild().getRoleById(adminRoleId).getName() + "`\t");
            }

            eb.addField("ADMIN", sb.toString(), false);
            sb = new StringBuilder();

            for (String superAdminRoleId : HifumiBot.getSelf().getConfig().permissions.superAdminRoleIds) {
                sb.append("`" + event.getGuild().getRoleById(superAdminRoleId).getName() + "`\t");
            }

            eb.addField("SUPER_ADMIN", sb.toString(), false);
            event.getHook().sendMessageEmbeds(eb.build()).queue();
            break;
        }
    }

    @Override
    protected CommandData defineSlashCommand() {
        OptionData permissionLevel = new OptionData(OptionType.STRING, "permission-level", "Permission level to modify", true)
                .addChoice("Blocked", "BLOCKED")
                .addChoice("Mod", "MOD")
                .addChoice("Admin", "ADMIN")
                .addChoice("Super Admin", "SUPER_ADMIN");
        OptionData role = new OptionData(OptionType.ROLE, "role", "Target role", true);
        
        SubcommandData add = new SubcommandData("add", "Add a role to a permission level")
                .addOptions(permissionLevel, role);
                
        SubcommandData remove = new SubcommandData("remove", "Remove a role from a permission level")
                .addOptions(permissionLevel, role);
        
        SubcommandData list = new SubcommandData("list", "List current role assignments to permission levels");
        
        return Commands.slash("perms", "Manage permission levels")
                .addSubcommands(add, remove, list)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

}
