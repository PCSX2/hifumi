// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.slash;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.config.ConfigManager;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.Strings;

public class CommandConfig extends AbstractSlashCommand {
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping pathOpt = event.getOption("path");
        OptionMapping valueOpt = event.getOption("value");
        
        if (pathOpt == null || valueOpt == null) {
            event.reply("Missing required options").setEphemeral(true).queue();
            return;
        }
        
        // Validate the path we received
        String path = pathOpt.getAsString();
        ArrayList<String> parts = new ArrayList<String>(Arrays.asList(StringUtils.split(path, '.')));
        
        Object obj = HifumiBot.getSelf().getConfig();
        
        while (!parts.isEmpty()) {
            String node = parts.get(0);
            Class<?> cls = obj.getClass();
            
            for (Field field : cls.getDeclaredFields()) {
                if (field.getName().equals(node)) {
                    try {
                        if (parts.size() > 1) {
                            obj = field.get(obj);
                            parts.remove(0);
                        } else {
                            String sub = event.getSubcommandName();
                            
                            
                            field.setAccessible(true);
                            
                            switch (sub) {
                                case "string": {
                                    field.set(obj, Strings.unescapeNewlines(valueOpt.getAsString()));
                                    break;
                                }
                                case "long": {
                                    field.setLong(obj, valueOpt.getAsLong());
                                    break;
                                }
                                case "int": {
                                    field.setInt(obj, valueOpt.getAsInt());
                                    break;
                                }
                                case "double": {
                                    field.setDouble(obj, valueOpt.getAsDouble());
                                    break;
                                }
                                case "bool": {
                                    field.setBoolean(obj, valueOpt.getAsBoolean());
                                    break;
                                }
                                default: {
                                    break;
                                }
                            }
                            
                            ConfigManager.write(HifumiBot.getSelf().getConfig());
                            event.reply("Successfully set config").setEphemeral(true).queue();
                            return;
                        }
                    } catch (IllegalAccessException e) {
                        Messaging.logException(e);
                        event.reply("An exception occurred while trying to set config").setEphemeral(true).queue();
                        return;
                    }
                }
            }
        }
        
        event.reply("Invalid path").setEphemeral(true).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        OptionData path = new OptionData(OptionType.STRING, "path", "The JSON path to set, dot separated.", true);
        SubcommandData string = new SubcommandData("string", "Set a string value")
                .addOptions(path)
                .addOption(OptionType.STRING, "value", "The value to set", true);
        SubcommandData lng = new SubcommandData("long", "Set a long value")
                .addOptions(path)
                .addOption(OptionType.INTEGER, "value", "The value to set", true);
        SubcommandData it = new SubcommandData("int", "Set a long value")
                .addOptions(path)
                .addOption(OptionType.INTEGER, "value", "The value to set", true);
        SubcommandData dbl = new SubcommandData("double", "Set a long value")
                .addOptions(path)
                .addOption(OptionType.NUMBER, "value", "The value to set", true);
        SubcommandData bool = new SubcommandData("bool", "Set a boolean value")
                .addOptions(path)
                .addOption(OptionType.BOOLEAN, "value", "The value to set", true);
        CommandData ret = Commands.slash("config", "Interface to edit configuration from within Discord")
                .addSubcommands(string, lng, it, dbl, bool)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
        return ret;
    }
}
