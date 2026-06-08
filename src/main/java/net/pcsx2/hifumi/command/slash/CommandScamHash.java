package net.pcsx2.hifumi.command.slash;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.database.Database;

public class CommandScamHash extends AbstractSlashCommand {

    private static final String SHA256_REGEX = "[A-Fa-f0-9]{64}";
    
    private Pattern shaPattern;
    
    public CommandScamHash() {
        this.shaPattern = Pattern.compile(SHA256_REGEX);
    }
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping shaOpt = event.getOption("sha256");
        OptionMapping descriptionOpt = event.getOption("description");
        
        if (shaOpt == null || descriptionOpt == null) {
            event.reply("Missing required params").setEphemeral(true).queue();
            return;
        }
        
        String sha256 = shaOpt.getAsString();
        Matcher m = shaPattern.matcher(sha256);
        
        if (!m.matches()) {
            event.reply("Not a valid SHA-256 sum").setEphemeral(true).queue();
            return;
        }
        
        String description = descriptionOpt.getAsString();
        Database.insertScamHash(sha256, description);
        event.reply("Added SHA-256 sum").setEphemeral(true).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("scamhash", "Registers a SHA-256 sum as a scam image")
                .addOption(OptionType.STRING, "sha256", "SHA-256 sum of the image file", true)
                .addOption(OptionType.STRING, "description", "A text descriptor of what the image content is", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

}
