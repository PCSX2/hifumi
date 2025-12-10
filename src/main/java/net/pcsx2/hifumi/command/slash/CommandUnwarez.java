/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package net.pcsx2.hifumi.command.slash;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Optional;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.config.Config.SerializedEmbedField;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.InteractionEventObject;
import net.pcsx2.hifumi.util.MemberUtils;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.RoleUtils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 *
 * @author pandubz
 */
public class CommandUnwarez extends AbstractSlashCommand {

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping userOpt = event.getOption("user");

        if (userOpt == null) {
            event.reply("Could not unwarez; no user specified").setEphemeral(true).queue();
            return;
        }

        User user = userOpt.getAsUser();
        Optional<Member> memberOpt = MemberUtils.getOrRetrieveMember(event.getGuild(), user.getIdLong());

        if (memberOpt.isEmpty()) {
            event.reply("User appears to have left the server.").setEphemeral(true).queue();
            return;
        }

        boolean hasWarezRole = false;

        for (Role role : memberOpt.get().getRoles()) {
            if (role.getId().equals(HifumiBot.getSelf().getConfig().roles.warezRoleId)) {
                hasWarezRole = true;
                break;
            }
        }

        if (!hasWarezRole) {
            event.reply("User is not warez.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(HifumiBot.getSelf().getConfig().unwarezPrompt.title);
        eb.setDescription(HifumiBot.getSelf().getConfig().unwarezPrompt.body);
        eb.setColor(Color.YELLOW);
        
        for (SerializedEmbedField field : HifumiBot.getSelf().getConfig().unwarezPrompt.fields) {
            eb.addField(field.name, field.value, field.inline);
        }

        ArrayList<Button> buttons = new ArrayList<Button>();
        buttons.add(
            Button.primary(
                "unwarez:" + event.getId() + ":" + user.getId(), 
                "By clicking, I understand and agree to the above"
            )
        );

        Database.insertInteractionEvent(event.getIdLong(), event.getTimeCreated().toEpochSecond(), user.getIdLong());
        
        event.reply(user.getAsMention())
            .addEmbeds(eb.build())
            .addComponents(ActionRow.of(buttons))
            .queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("unwarez", "Prompt a user that they are eligible to remove their warez role")
                .addOption(OptionType.USER, "user", "User to remove warez role from", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES));
    }

    @Override 
    public void handleButtonEvent(ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        String[] parts = componentId.split(":");

        if (parts.length != 3) {
            Messaging.logInfo("CommandUnwarez", "handleButtonEvent", "Received a button click event, but got a malformed button ID. Received:\n```\n" + componentId + "\n```");
            event.getHook().sendMessage("Malformed button ID, admins have been notified.").setEphemeral(true).queue();
            return;
        }

        Long eventIdLong;
        Long userIdLong;

        try {
            eventIdLong = Long.valueOf(parts[1]);
            userIdLong = Long.valueOf(parts[2]);
        } catch (NumberFormatException e) {
            Messaging.logInfo("CommandUnwarez", "handleButtonEvent", "Possible tampering in button ID. Received:\n```\n" + componentId + "\n```");
            event.getHook().sendMessage("Malformed parameters in button ID, admins have been notified.").setEphemeral(true).queue();
            return;
        }

        if (userIdLong != event.getUser().getIdLong()) {
            event.getHook().sendMessage("This button is not for you to use.").setEphemeral(true).queue();
            return;
        }

        Optional<InteractionEventObject> interactionEventOpt = Database.getInteractionEvent(eventIdLong, userIdLong);

        if (interactionEventOpt.isEmpty()) {
            Messaging.logInfo("CommandUnwarez", "handleButtonEvent", "No valid event found:\n```\n" + componentId + "\n```");
            event.getHook().sendMessage("No valid event found, admins have been notified.").setEphemeral(true).queue();
            return;
        }

        Guild server = event.getGuild();
        Member member = event.getMember();
        String warezRoleId = HifumiBot.getSelf().getConfig().roles.warezRoleId;
        
        if (RoleUtils.memberHasRole(member, warezRoleId)) {
            event.getHook().sendMessage("You do not have the warez role.").setEphemeral(true).queue();
            return;
        }

        if (server != null && member != null) {
            Role warezRole = server.getRoleById(warezRoleId);

            if (warezRole != null) {
                server.removeRoleFromMember(member, warezRole).queue();
                Button acceptedButton = Button.secondary("null", "User accepted, warez removed").asDisabled();
                ActionRow newRow = ActionRow.of(acceptedButton);
                event.getHook().editOriginalComponents(newRow).queue();
            } else {
                Messaging.logInfo("CommandUnwarez", "handleButtonEvent", "Warez role not located, is it configured properly?");
            }
        } else {
            Messaging.logInfo("CommandUnwarez", "handleButtonEvent", "Server or member instance lost, something is very wrong!");
        }
    }
}
