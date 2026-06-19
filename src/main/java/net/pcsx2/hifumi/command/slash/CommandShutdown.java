// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.slash;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandShutdown extends AbstractSlashCommand {
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        HifumiBot.getSelf().getScheduler().runOnce(() -> {
            if (HifumiBot.getSelf() != null)
                HifumiBot.getSelf().shutdown(false);
        });
        event.reply("Shutting down, bye bye!").queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("shutdown", "Shuts down the bot with no attempt to reload")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

}
