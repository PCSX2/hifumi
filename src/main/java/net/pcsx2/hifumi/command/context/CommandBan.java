package net.pcsx2.hifumi.command.context;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractUserContextCommand;
import net.pcsx2.hifumi.permissions.PermissionLevel;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.modals.Modal;

public class CommandBan extends AbstractUserContextCommand {
    
    @Override
    protected void onExecute(UserContextInteractionEvent event) {
        if (!event.getMember().hasPermission(Permission.BAN_MEMBERS)) {
            event.reply("You do not have permission to ban users.").setEphemeral(true);
            return;
        }

        User user = event.getTarget();
        Member member = event.getTargetMember();

        if (member != null && HifumiBot.getSelf().getPermissionManager().hasPermission(PermissionLevel.MOD, member)) {
            event.reply("This user has elevated permissions. To minimize accidents, elevated users cannot be banned with this command.").setEphemeral(true).queue();
            return;
        }

        if (user != null) {
            TextInput.Builder userId = TextInput.create("userid", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setValue(user.getId());
            Label userIdLabel = Label.of("User ID (DO NOT EDIT)", userId.build());
            TextInput.Builder username = TextInput.create("username", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setValue(user.getName());
            Label usernameLabel = Label.of("Username (DO NOT EDIT)", username.build());
            TextInput.Builder displayName = TextInput.create("displayname", TextInputStyle.SHORT)
                    .setRequired(true)
                    .setValue(user.getEffectiveName());
            Label displayNameLabel = Label.of("Current Display Name (DO NOT EDIT)", displayName.build());
            Modal.Builder modal;
            modal = Modal.create("ban", "Confirm Banning Member")
                    .addComponents(
                            userIdLabel,
                            usernameLabel,
                            displayNameLabel
                    );
            event.replyModal(modal.build()).queue();
        }
    }

    @Override
    protected CommandData defineUserContextCommand() {
        return Commands.user("ban")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS));
    }
}
