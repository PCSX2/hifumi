package net.pcsx2.hifumi.command.slash;

import java.awt.Color;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.pcsx2.hifumi.BrowsableEmbed;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandServerMetadata extends AbstractSlashCommand {

    @Override 
    public void onExecute(SlashCommandInteractionEvent event) {
        event.deferReply().setEphemeral(true).queue();

        ArrayList<MessageEmbed> pages = new ArrayList<MessageEmbed>();
        Guild server = HifumiBot.getSelf().getJDA().getGuildById(event.getGuild().getIdLong());

        // General info page
        EmbedBuilder generalEmbedBuilder;
        generalEmbedBuilder = new EmbedBuilder();
        generalEmbedBuilder.setColor(Color.BLUE);
        generalEmbedBuilder.setTitle("General Info");
        generalEmbedBuilder.setImage(server.getIconUrl());
        generalEmbedBuilder.addField("Server ID", server.getId(), true);
        generalEmbedBuilder.addField("Server Name", server.getName(), true);
        generalEmbedBuilder.addField("Created", server.getTimeCreated().format(DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss")) + " UTC", true);
        generalEmbedBuilder.addField("Members", String.valueOf(server.getMemberCount()), true);
        pages.add(generalEmbedBuilder.build());

        // Channel list page
        List<GuildChannel> channels = server.getChannels();

        ArrayList<ArrayList<GuildChannel>> channelsPaginated = new ArrayList<ArrayList<GuildChannel>>();
        ArrayList<GuildChannel> buildingPage =  new ArrayList<GuildChannel>();

        for (GuildChannel channel : channels) {
            buildingPage.add(channel);

            if (buildingPage.size() >= 8) {
                channelsPaginated.add(buildingPage);
                buildingPage = new ArrayList<GuildChannel>();
            }
        }

        if (!buildingPage.isEmpty()) {
            channelsPaginated.add(buildingPage);
        }

        int i = 1;

        for (ArrayList<GuildChannel> channelPage : channelsPaginated) {
            EmbedBuilder channelsEmbedBuilder = new EmbedBuilder();
            channelsEmbedBuilder.setColor(Color.GREEN);
            channelsEmbedBuilder.setTitle("Channels (Page " + i++ + ")");

            for (GuildChannel channel : channelPage) {
                channelsEmbedBuilder.addField("Name", channel.getName(), true);
                channelsEmbedBuilder.addField("ID", channel.getId(), true);
                channelsEmbedBuilder.addField("Created", channel.getTimeCreated().format(DateTimeFormatter.ofPattern("MMM dd yyyy HH:mm:ss")) + " UTC", true);
            }

            channelsEmbedBuilder.setFooter("Channels are listed in the order they are returned by Discord.");
            pages.add(channelsEmbedBuilder.build());
        }

        BrowsableEmbed embed = new BrowsableEmbed(this.defineSlashCommand().getName(), event.getIdLong(), event.getUser().getIdLong(), pages);

        Optional<MessageEmbed> firstPageOpt = embed.getCurrentPage();

        if (firstPageOpt.isPresent()) {
            ArrayList<Button> buttons = embed.refreshButtonOptions();
            BrowsableEmbed.embedCache.put(event.getIdLong(), embed);
            event.getHook().sendMessageEmbeds(firstPageOpt.get())
                    .addComponents(ActionRow.of(buttons))
                    .queue();
        } else {
            event.getHook().editOriginal("Failed to generate embeds").queue();
        }
    }

    @Override 
    protected CommandData defineSlashCommand() {
        return Commands.slash("server-metadata", "Fetch general server metadata")
            .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

    public void handleButtonEvent(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        
        String[] parts = componentId.split(":");

        if (parts.length != 4) {
            return;
        }

        String action = parts[1];
        Long eventId = null;

        try {
            eventId = Long.valueOf(parts[2]);
        } catch (Exception e) {
            event.getHook().sendMessage("Malformed event ID").setEphemeral(true).queue();
            return;
        }
        
        String userId = parts[3];

        // Test if the event that originally created the browsable embed is in memory.
        BrowsableEmbed existingBrowsableEmbed = BrowsableEmbed.embedCache.get(eventId);

        if (existingBrowsableEmbed == null) {
            event.getHook().sendMessage("This embed has expired, you should re-run the original command instead").setEphemeral(true).queue();
            return;
        }

        Optional<MessageEmbed> destinationPage = Optional.empty();

        switch (action) {
            case "prev": {
                destinationPage = existingBrowsableEmbed.prevPage();
                break;
            }
            case "next": {
                destinationPage = existingBrowsableEmbed.nextPage();
                break;
            }
            default: {
                break;
            }
        }

        if (destinationPage.isPresent()) {
            ArrayList<Button> buttons = existingBrowsableEmbed.refreshButtonOptions();
            event.getHook().editOriginalEmbeds(destinationPage.get())
                .setComponents(ActionRow.of(buttons))
                .queue();
        }
    }
}
