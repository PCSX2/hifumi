// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.slash;

import java.util.Optional;

import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.util.MemberUtils;
import net.pcsx2.hifumi.util.WarezUtil;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandWarez extends AbstractSlashCommand {
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        OptionMapping userOpt = event.getOption("user");
        
        if (userOpt == null) {
            event.getHook()
                .sendMessage("Could not record warez; no user specified")
                .setEphemeral(true)
                .queue();
            return;
        }
        
        User user = event.getOption("user").getAsUser();
        Optional<Member> memberOpt = MemberUtils.getOrRetrieveMember(event.getGuild(), user.getIdLong());
        Optional<Message> messageOpt = Optional.empty();

        WarezUtil.applyWarez(event, user, memberOpt, messageOpt);
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("warez", "Show a prompt about anti-piracy rules and assign warez role")
                .addOption(OptionType.USER, "user", "User to assign warez role to", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES));
    }
}
