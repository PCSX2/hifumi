package net.pcsx2.hifumi.command.slash;

import java.awt.Color;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.async.MessageBulkDeleteTargetedRunnable;
import net.pcsx2.hifumi.command.AbstractSlashCommand;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CommandBulkDelete extends AbstractSlashCommand {

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping userOpt = event.getOption("user");
        OptionMapping channelOpt = event.getOption("channel");
        OptionMapping messageContentOpt = event.getOption("message-content");
        OptionMapping hoursOpt = event.getOption("hours");

        if (userOpt == null) {
            event.reply("Missing parameter `user`").setEphemeral(true).queue();
            return;
        }

        User user = userOpt.getAsUser();

        if (channelOpt == null) {
            event.reply("Missing parameter `channel`").setEphemeral(true).queue();
            return;
        }

        TextChannel channel = null;

        try {
            channel = channelOpt.getAsChannel().asTextChannel();
        } catch (Exception e) {
            event.reply("Channel was not a text channel").setEphemeral(true).queue();
            return;
        }

        if (messageContentOpt == null) {
            event.reply("Missing parameter `messageContent`").setEphemeral(true).queue();
            return;
        }

        String messageContent = messageContentOpt.getAsString();

        if (hoursOpt == null) {
            event.reply("Missing parameter `hours`").setEphemeral(true).queue();
            return;
        }

        int hours = hoursOpt.getAsInt();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Bulk Delete Action");
        eb.setColor(Color.RED);
        eb.addField("Initiated By", event.getUser().getAsMention(), false);
        eb.addField("Target User", user.getAsMention(), false);
        eb.addField("Messages Containing", messageContent, false);
        eb.addField("Hours to Search", String.valueOf(hours), false);
        event.replyEmbeds(eb.build()).queue();

        MessageBulkDeleteTargetedRunnable runnable = new MessageBulkDeleteTargetedRunnable(event.getGuild().getId(), user.getId(), channel.getId(), messageContent, hours);
        HifumiBot.getSelf().getScheduler().runOnce(runnable);
    }

    @Override
    protected CommandData defineSlashCommand() {
        OptionData user = new OptionData(OptionType.USER, "user", "Target User").setRequired(true);
        OptionData channel = new OptionData(OptionType.CHANNEL, "channel", "Target Channel").setRequired(true);
        OptionData messageContent = new OptionData(OptionType.STRING, "message-content", "Message content to look for - ran through a basic 'String.contains(String)' function").setRequired(true);
        OptionData hours = new OptionData(OptionType.INTEGER, "hours", "How many hours backwards to search").setRequired(true);

        return Commands.slash("bulkdelete", "Targeted bulk delete of specific message content")
                .addOptions(user, channel, messageContent, hours)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }
    
}
