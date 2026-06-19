// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.context;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import net.pcsx2.hifumi.command.AbstractMessageContextCommand;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandReverseImage extends AbstractMessageContextCommand {

    private static final String URL_FORMAT = "https://lens.google.com/uploadbyurl?url=%s";
    
    @Override
    protected void onExecute(MessageContextInteractionEvent event) {
        event.deferReply(true).queue();
        Message msg = event.getTarget();
        
        if (msg.getAttachments().isEmpty()) {
            event.getHook().sendMessage("No images found to do a reverse search on!").queue();
            return;
        }
        
        try {
            ArrayList<Button> buttons = new ArrayList<Button>();
            buttons.add(Button.link(event.getTarget().getJumpUrl(), "Jump to Original Message"));
            
            for (Attachment attach : msg.getAttachments()) {
                if (attach.isImage()) {
                    String encoded = URLEncoder.encode(attach.getProxyUrl(), "UTF-8");
                    String url = String.format(URL_FORMAT, encoded);
                    buttons.add(Button.link(url, String.format("Image %d", buttons.size())));
                }
            }
            
            event.getHook().sendMessage("Generated Google Lens links for all images in this message!")
                    .addComponents(ActionRow.of(buttons)).queue();
        } catch (UnsupportedEncodingException e) {
            event.getHook().sendMessage("Could not prepare one or more Google Lens link for this message, perhaps there are some weird characters in one of the URLs?").queue();
        }
    }

    @Override
    protected CommandData defineMessageContextCommand() {
        return Commands.message("reverse-image")
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }
}
