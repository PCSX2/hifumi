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
