package net.pcsx2.hifumi.command.slash;

import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.util.WarezUtil;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandWarezHistory extends AbstractSlashCommand {

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping userIdOpt = event.getOption("user-id");

        if (userIdOpt == null) {
            event.reply("Missing required option 'user-id'").setEphemeral(true).queue();
            return;
        }

        try {
            userIdOpt.getAsLong();
        } catch (Exception e) {
            event.reply("Bad ID format, make sure it's actually a number").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(true).queue();
        MessageEmbed embed = WarezUtil.createWarezHistoryEmbed(userIdOpt.getAsLong());
        event.getHook().editOriginalEmbeds(embed).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("warez-history", "Fetch a list of warez events for a user")
                .addOption(OptionType.STRING, "user-id", "User ID number to fetch warez history for", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES));
    }

}
