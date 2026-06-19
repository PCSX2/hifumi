// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
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
