/**
 * This file is part of HifumiBot, licensed under the MIT License (MIT)
 * 
 * Copyright (c) 2020 RedPanda4552 (https://github.com/RedPanda4552)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
