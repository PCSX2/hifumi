package net.pcsx2.hifumi.command.slash;

import net.pcsx2.hifumi.command.AbstractSlashCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.modals.Modal;

public class CommandPrompt extends AbstractSlashCommand {

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        TextInput titleInput = TextInput.create("title", TextInputStyle.SHORT)
                .setMinLength(1)
                .setMaxLength(MessageEmbed.TITLE_MAX_LENGTH)
                .setRequired(true)
                .build();
        Label titleInputLabel = Label.of("Set Title", titleInput);
        TextInput bodyInput = TextInput.create("body", TextInputStyle.PARAGRAPH)
                .setMinLength(1)
                .setMaxLength(4000)
                .setRequired(true)
                .build();
        Label bodyInputLabel = Label.of("Set Body Content", bodyInput);
        Modal modal = Modal.create("prompt", "Make a new prompt")
                .addComponents(titleInputLabel, bodyInputLabel)
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("prompt", "Open text inputs to generate a custom embed")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

}
