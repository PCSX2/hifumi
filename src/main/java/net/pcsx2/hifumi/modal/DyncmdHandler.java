package net.pcsx2.hifumi.modal;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.dynamic.DynamicChoice;
import net.pcsx2.hifumi.command.dynamic.DynamicCommand;
import net.pcsx2.hifumi.command.dynamic.DynamicSubcommand;
import net.pcsx2.hifumi.config.ConfigManager;
import net.pcsx2.hifumi.permissions.PermissionLevel;
import net.pcsx2.hifumi.util.EmbedUtil;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;

public class DyncmdHandler {

    public static void handle(ModalInteractionEvent event) {
        // Permissions check first
        if (!HifumiBot.getSelf().getPermissionManager().hasPermission(PermissionLevel.MOD, event.getMember())) {
            Messaging.logInfo("DyncmdHandler", "handle", "User " + event.getUser().getAsMention() + " tried to send a modal interaction for dyncmd, but does not have permission.");
            event.reply("Permissions error, staff have been notified").setEphemeral(true).queue();
            return;
        }

        // Unpack all our strings from the modal
        String fullCommand = event.getValue("command").getAsString();
        String[] parts = fullCommand.split(" ");

        if (parts.length != 3) {
            event.reply("Malformed command name")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        String commandStr = parts[0];
        String subcommandStr = parts[1];
        String choiceStr = parts[2];

        String descriptionStr = event.getValue("description").getAsString();
        String titleStr = event.getValue("title").getAsString();
        String bodyStr = event.getValue("body").getAsString();
        String imageStr = event.getValue("image").getAsString();

        // If imageStr is empty string, clear it to null to prevent URL validation from failing in other places later.
        if (imageStr != null && imageStr.isBlank()) {
            imageStr = null;
        }

        // Fetch the existing top level command, if relevant.
        DynamicCommand command = HifumiBot.getSelf().getDynCmdConfig().dynamicCommands.get(commandStr);
        DynamicSubcommand subcommand = null;

        // Then also look for the subcommand if it might exist.
        if (command != null) {
            subcommand = command.getSubcommand(subcommandStr);
        }

        // Always make a new choice, we are overwriting this every time.
        DynamicChoice choice = new DynamicChoice(choiceStr, descriptionStr, titleStr, bodyStr, imageStr);
        
        // If we did not find an existing subcommand, make one now.
        if (subcommand == null) {
            subcommand = new DynamicSubcommand(subcommandStr, descriptionStr);
        }

        subcommand.putChoice(choice);
        
        // If we did not find an existing command, make one now.
        if (command == null) {
            command = new DynamicCommand(commandStr, descriptionStr);
        }

        command.putSubcommand(subcommand);
        
        // Commit it to the map in memory, save the config, rebuild the command index
        HifumiBot.getSelf().getDynCmdConfig().dynamicCommands.put(commandStr, command);
        ConfigManager.write(HifumiBot.getSelf().getDynCmdConfig());
        HifumiBot.getSelf().getCommandIndex().rebuild();
        
        // Show a pretty embed with the new command details
        EmbedBuilder eb = EmbedUtil.getDynamicCommandEmbedBuilder(choice);
        event.replyEmbeds(eb.build()).queue();
    }
}
