package net.pcsx2.hifumi.command.slash;

import java.awt.Color;
import java.util.HashMap;

import com.deepl.api.Language;
import com.deepl.api.TextResult;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

public class CommandTranslate extends AbstractSlashCommand {

    private HashMap<String, String> supportedLanguages;

    public CommandTranslate() {
        this.supportedLanguages = new HashMap<String, String>();
        
        try {
            for (Language lang : HifumiBot.getSelf().getDeepL().getTargetLanguages()) {
                supportedLanguages.put(lang.getCode(), lang.getName());
            }
        } catch (Exception e) {
            Messaging.logException("CommandTranslate", "(constructor)", e);
        }
    }

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping langOpt = event.getOption("lang");
        OptionMapping textOpt = event.getOption("text");
        OptionMapping userOpt = event.getOption("user");
        
        if (langOpt == null) {
            event.reply("Required option `lang` missing").setEphemeral(true).queue();
            return;
        }

        String lang = langOpt.getAsString();

        if (!this.supportedLanguages.containsKey(lang)) {
            event.reply("Unsupported language. Please use a valid ISO language code:\n" + this.supportedLanguages.keySet().toString()).setEphemeral(true).queue();
            return;
        }
        
        if (textOpt == null) {
            event.reply("Required option `text` missing").setEphemeral(true).queue();
            return;
        }

        String text = textOpt.getAsString();
        
        Member member = null;

        if (userOpt != null) {
            member = userOpt.getAsMember();
        }

        event.deferReply().queue();
        TextResult res = null;

        try {
            res = HifumiBot.getSelf().getDeepL().translateText(text, null, lang);
        } catch (Exception e) {
            Messaging.logException("CommandTranslate", "onExecute", e);
            event.getHook().editOriginal("An error occurred while trying to translate. Admins have been notified.").queue();
            return;
        }

        if (res != null) {
            String translated = res.getText();

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("DeepL Translation");
            eb.setDescription(translated);
            eb.setColor(Color.MAGENTA);
            eb.setFooter("Target Language (ISO Code): " + lang);
            MessageEditBuilder mb = new MessageEditBuilder();
            
            if (member != null) {
                mb.setContent(member.getAsMention());
            }

            mb.setEmbeds(eb.build());
            event.getHook().editOriginal(mb.build()).queue();
        } else {
            event.getHook().editOriginal("An unknown error occurred. Please try again in a few minutes.").queue();
        }
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("translate", "Translate some text using DeepL")
                .addOption(OptionType.STRING, "lang", "Target language. Standard ISO codes (en-US, fr, es)", true)
                .addOption(OptionType.STRING, "text", "Text to translate", true)
                .addOption(OptionType.USER, "user", "User to ping")
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }
}
