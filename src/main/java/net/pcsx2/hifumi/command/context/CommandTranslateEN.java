// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.context;

import java.awt.Color;

import com.deepl.api.TextResult;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractMessageContextCommand;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandTranslateEN extends AbstractMessageContextCommand {

    @Override
    protected void onExecute(MessageContextInteractionEvent event) {
        event.deferReply(true).queue();
        String content = event.getTarget().getContentDisplay();
        event.getHook().editOriginal(":hourglass: Sending a translation request to DeepL... This may take a moment...").queue();
        
        TextResult res = null;
        
        try {
            res = HifumiBot.getSelf().getDeepL().translateText(content, null, "en-US");
        } catch (Exception e) {
            Messaging.logException("CommandTranslateEN", "onExecute", e);
            event.getHook().editOriginal("An error occurred while trying to translate. Admins have been notified.").queue();
            return;
        }

        if (res != null) {
            String translated = res.getText();
            String sourceLang = res.getDetectedSourceLanguage();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("DeepL Translation");
            eb.setDescription(translated);
            eb.setColor(Color.BLUE);
            eb.setFooter("Source Language (ISO Code): " + sourceLang);
            event.getTarget().replyEmbeds(eb.build()).mentionRepliedUser(false).queue();

            try {
                event.getHook().deleteOriginal().queue();    
            } catch (Exception e) {
                // Squelch
            }
        } else {
            event.getHook().editOriginal("An unknown error occurred. Please try again in a few minutes.").queue();
        }
    }

    @Override
    protected CommandData defineMessageContextCommand() {
        return Commands.message("translate-en")
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }
}
