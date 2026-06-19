// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.slash;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.ScamHashObject;

public class CommandScamHash extends AbstractSlashCommand {

    private static final String SHA256_REGEX = "[A-Fa-f0-9]{64}";
    
    private Pattern shaPattern;
    
    public CommandScamHash() {
        this.shaPattern = Pattern.compile(SHA256_REGEX);
    }
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        switch (event.getSubcommandName()) {
            case "add" -> {
                OptionMapping shaOpt = event.getOption("sha3-256");
                OptionMapping descriptionOpt = event.getOption("description");
                
                if (shaOpt == null || descriptionOpt == null) {
                    event.reply("Missing required params").setEphemeral(true).queue();
                    return;
                }
                
                String sha256 = shaOpt.getAsString();
                Matcher m = shaPattern.matcher(sha256);
                
                if (!m.matches()) {
                    event.reply("Not a valid SHA3-256 sum").setEphemeral(true).queue();
                    return;
                }
                
                Optional<ScamHashObject> existing = Database.getScamHash(sha256);
                
                if (existing.isPresent()) {
                    event.reply("Matching hash already exists, use deactivate or reactivate to make changes").setEphemeral(true).queue();
                    return;
                }
                
                String description = descriptionOpt.getAsString();
                Database.insertScamHash(sha256, description);
                event.reply("Added SHA3-256 sum").setEphemeral(true).queue();
                break;
            }
            case "deactivate" -> {
                OptionMapping shaOpt = event.getOption("sha3-256");
                
                if (shaOpt == null) {
                    event.reply("Missing required params").setEphemeral(true).queue();
                    return;
                }
                
                String sha256 = shaOpt.getAsString();
                Matcher m = shaPattern.matcher(sha256);
                
                if (!m.matches()) {
                    event.reply("Not a valid SHA3-256 sum").setEphemeral(true).queue();
                    return;
                }
                
                Database.updateScamHash(sha256, false);
                event.reply("Deactivated SHA3-256 sum " + sha256).setEphemeral(true).queue();
                break;
            }
            case "reactivate" -> {
                OptionMapping shaOpt = event.getOption("sha3-256");
                OptionMapping descriptionOpt = event.getOption("description");
                
                if (shaOpt == null || descriptionOpt == null) {
                    event.reply("Missing required params").setEphemeral(true).queue();
                    return;
                }
                
                String sha256 = shaOpt.getAsString();
                Matcher m = shaPattern.matcher(sha256);
                
                if (!m.matches()) {
                    event.reply("Not a valid SHA3-256 sum").setEphemeral(true).queue();
                    return;
                }
                
                String description = descriptionOpt.getAsString();
                Database.updateScamHash(sha256, true, description);
                event.reply("Reactivated SHA3-256 sum " + sha256).setEphemeral(true).queue();
                break;
            }
            case "get" -> {
                OptionMapping shaOpt = event.getOption("sha3-256");
                
                if (shaOpt == null) {
                    event.reply("Missing required params").setEphemeral(true).queue();
                    return;
                }
                
                String sha256 = shaOpt.getAsString();
                Matcher m = shaPattern.matcher(sha256);
                
                if (!m.matches()) {
                    event.reply("Not a valid SHA3-256 sum").setEphemeral(true).queue();
                    return;
                }
                
                Optional<ScamHashObject> scamHashOpt = Database.getScamHash(sha256);
                
                if (scamHashOpt.isPresent()) {
                    ScamHashObject scamHashObj = scamHashOpt.get();
                    StringBuilder sb = new StringBuilder()
                            .append("SHA3-256: ")
                            .append(scamHashObj.getSHA256())
                            .append("\nDescription: ")
                            .append(scamHashObj.getDescription())
                            .append("\nActive: ")
                            .append(scamHashObj.isActive());
                    event.reply(sb.toString()).setEphemeral(true).queue();
                } else {
                    event.reply("No hash found with SHA3-256 of " + sha256).setEphemeral(true).queue();
                }

                break;
            }
            default -> {
                event.reply("Unexpected subcommand").setEphemeral(true).queue();
                return;
            }
        }
        
    }

    @Override
    protected CommandData defineSlashCommand() {
        OptionData sha256 = new OptionData(OptionType.STRING, "sha3-256", "SHA-256 sum of the image file", true);
        OptionData description = new OptionData(OptionType.STRING, "description", "A text descriptor of what the image content is", true);
        
        SubcommandData add = new SubcommandData("add", "Add a new hash in SHA3-256 format")
                .addOptions(sha256, description);
        
        SubcommandData deactivate = new SubcommandData("deactivate", "Deactivate a hash so it will no longer trigger")
                .addOptions(sha256);
        
        SubcommandData reactivate = new SubcommandData("reactivate", "Reactivate a hash and update its description")
                .addOptions(sha256, description);
        
        SubcommandData get = new SubcommandData("get", "Get a hash and its description")
                .addOptions(sha256);
        
        return Commands.slash("scamhash", "Registers a SHA3-256 sum as a scam file hash")
                .addSubcommands(add, deactivate, reactivate, get)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

}
