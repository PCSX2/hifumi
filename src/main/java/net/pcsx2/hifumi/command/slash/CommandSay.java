// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.slash;

import net.pcsx2.hifumi.command.AbstractSlashCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.modals.Modal;

public class CommandSay extends AbstractSlashCommand {

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        TextInput bodyInput = TextInput.create("body", TextInputStyle.PARAGRAPH)
                .setMinLength(1)
                .setMaxLength(Message.MAX_CONTENT_LENGTH)
                .setRequired(true)
                .build();
        Label bodyLabel = Label.of("Set Body Content", bodyInput);

        Modal modal = Modal.create("say", "Make the bot say something")
                .addComponents(bodyLabel)
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("say", "Control basic bot message sending")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
                
    }
}
