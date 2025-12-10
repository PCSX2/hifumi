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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandSerial extends AbstractSlashCommand {

    @SuppressWarnings("unchecked")
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping opt = event.getOption("name");
        
        if (opt == null) {
            Messaging.logInfo("CommandSerial", "onExecute", "Command tampering? Missing option 'name' (user = " + event.getUser().getAsMention() + ")");
            event.reply("Invalid option detected, admins have been alerted.").setEphemeral(true).queue();
            return;
        }

        if (!HifumiBot.getSelf().getGameIndex().isInitialized()) {
            event.reply("Whoa there! The bot is still fetching data from GameIndex.yaml, please wait a moment while that finishes!").queue();
            return;
        }

        String normalized = opt.getAsString().toLowerCase();

        event.deferReply().setEphemeral(true).queue();
        event.getHook().editOriginal(":information_source: Checking GameIndex.yaml for serials matching name `" + normalized + "`, this might take a moment...").queue();
        
        Map<String, Object> indexMap = HifumiBot.getSelf().getGameIndex().getMap();
        HashMap<String, LinkedHashMap<String, String>> results = new HashMap<String, LinkedHashMap<String, String>>();

        for (String serial : indexMap.keySet()) {
            Map<String, Object> entryMap = (Map<String, Object>) indexMap.get(serial);

            String name = ((String) entryMap.get("name"));
            String nameSort = null;
            String nameEn = null;
            String region = ((String) entryMap.get("region"));
            
            if (entryMap.containsKey("name-sort")) {
                nameSort = ((String) entryMap.get("name-sort"));
            }

            if (entryMap.containsKey("name-en")) {
                nameEn = ((String) entryMap.get("name-en"));
            }
            
            if (StringUtils.containsIgnoreCase(name, normalized) || StringUtils.containsIgnoreCase(nameSort, normalized) || StringUtils.containsIgnoreCase(nameEn, normalized)) {
                LinkedHashMap<String, String> attributes = new LinkedHashMap<String, String>();
                attributes.put("Name", name);

                if (nameEn != null) {
                    attributes.put("EN", nameEn);
                }
                
                attributes.put("Region", region);
                
                results.put(serial, attributes);
            }
        }
        
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Search Results for \"" + normalized + "\"");
        Set<String> serials = results.keySet();
        TreeSet<String> orderedSerials = new TreeSet<String>(serials);
        
        for (String serial : orderedSerials) {
            StringBuilder sb = new StringBuilder();
            LinkedHashMap<String, String> attributes = results.get(serial);

            for (String key : attributes.keySet()) {
                sb.append("* ").append(attributes.get(key)).append("\n");
            }

            eb.addField(serial, sb.toString().trim(), true);

            if (eb.getFields().size() >= 25) {
                eb.setDescription("More than 25 results found. Consider using a more specific search term if what you need is not here.");
                break;
            }
        }
        
        MessageEmbed embed = null;

        try {
            embed = eb.build();
        } catch (IllegalStateException e) {
            embed = new EmbedBuilder().setTitle("Too many results").setDescription("Your search returned too many results, it cannot be displayed. Please use a more concise search term.").build();
        }

        event.getHook().editOriginal("Done!").setEmbeds(embed).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("serial", "Look up serial numbers by providing part of a game name")
                .addOption(OptionType.STRING, "name", "Part of a game name to search for. Search will do a literal string compare.", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }
}
