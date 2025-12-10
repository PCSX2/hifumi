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
package net.pcsx2.hifumi.command.slash;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.imageio.ImageIO;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandPFP extends AbstractSlashCommand {
    
    private boolean setAvatar(String imageUrl) throws IOException, MalformedURLException {
        try {
            URL url = new URI(imageUrl).toURL();
            BufferedImage bImage = ImageIO.read(url);
            ByteArrayOutputStream oStream = new ByteArrayOutputStream();
            ImageIO.write(bImage, "png", oStream);
            HifumiBot.getSelf().getJDA().getSelfUser().getManager().setAvatar(Icon.from(oStream.toByteArray())).complete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        
        try {
            String imageUrl = event.getOption("image-url").getAsString();
            setAvatar(imageUrl);
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Avatar set!");
            eb.setDescription(imageUrl);
            eb.setImage(imageUrl);
            event.getHook().sendMessageEmbeds(eb.build()).queue();
        } catch (Exception e) {
            event.getHook().sendMessage("An error occurred while setting the avatar.").queue();
            Messaging.logException("CommandPFP", "onExecute", e);
        }
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("pfp", "Set the bot's avatar")
                .addOption(OptionType.STRING, "image-url", "URL pointing to the new avatar image", true)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }
}
