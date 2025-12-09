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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.FuzzyScore;

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.selections.SelectMenu;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandGameIndex extends AbstractSlashCommand {

    private static final Pattern GAMEINDEX_SERIAL_PATTERN = Pattern.compile("^[A-Z]{4}-[0-9]{5}$");
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping searchOpt = event.getOption("search");
        OptionMapping preferEnglishOpt = event.getOption("prefer-english");
        boolean preferEnglish = false;

        if (preferEnglishOpt != null) {
            preferEnglish = preferEnglishOpt.getAsBoolean();
        }
        
        if (searchOpt == null) {
            Messaging.logInfo("CommandGameIndex", "onExecute", "Command tampering? Missing option 'search' (user = " + event.getUser().getAsMention() + ")");
            event.reply("Invalid option detected, admins have been alerted.").setEphemeral(true).queue();
            return;
        }

        if (!HifumiBot.getSelf().getGameIndex().isInitialized()) {
            event.reply("Whoa there! The bot is still fetching data from GameIndex.yaml, please wait a moment while that finishes!").queue();
            return;
        }
        
        String normalized = searchOpt.getAsString().toUpperCase();
        Matcher m = GAMEINDEX_SERIAL_PATTERN.matcher(normalized);
        
        if (m.matches()) {
            MessageEmbed embed = HifumiBot.getSelf().getGameIndex().present(normalized);
            event.replyEmbeds(embed).queue();
        } else {
            event.deferReply().queue();
            event.getHook().editOriginal(":information_source: Checking GameIndex.yaml for game by name, this might take a moment...").queue();
            FuzzyScore fuzz = new FuzzyScore(Locale.US);

            HashMap<String, Integer> highScores = new HashMap<String, Integer>();

            Map<String, Object> mainGameIndexMap = HifumiBot.getSelf().getGameIndex().getMap();

            for (String serial : mainGameIndexMap.keySet()) {
                Map<String, Object> gameEntry = (Map<String, Object>) mainGameIndexMap.get(serial);
                String name = (String) gameEntry.get("name");            
                Integer nameScore = fuzz.fuzzyScore(normalized, name.toUpperCase());
                
                // Compare name-en if present
                if (gameEntry.containsKey("name-en")) {
                    String nameEnglish = (String) gameEntry.get("name-en");
                    Integer nameEnglishScore = fuzz.fuzzyScore(normalized, nameEnglish);

                    // If name-en was better, use it instead.
                    if (nameEnglishScore > nameScore) {
                        nameScore = nameEnglishScore;
                    }
                }

                // If score is horribly low, move on.
                if (nameScore < 10) {
                    continue;
                }

                // If at capacity already, compare scores. If higher score, make room. Else, skip.
                if (highScores.size() >= SelectMenu.OPTIONS_MAX_AMOUNT) {
                    String lowestSerial = null;
                    Integer lowestScore = Integer.MAX_VALUE;

                    for (String serialAgain : highScores.keySet()) {
                        Integer thisScore = highScores.get(serialAgain);

                        if (thisScore < lowestScore) {
                            lowestSerial = serialAgain;
                            lowestScore = thisScore;
                        }
                    }

                    if (nameScore > lowestScore) {
                        highScores.remove(lowestSerial);
                    } else {
                        continue;
                    }
                }

                highScores.put(serial, nameScore);
            }

            if (highScores.isEmpty()) {
                event.getHook().sendMessage("No results found, please check spelling and refine your search, or use a serial number to search by.").queue();
                return;
            }

            // Sort the high scores so we can present them from closest to farthest.
            ArrayList<Entry<String, Integer>> entryList = new ArrayList<Entry<String, Integer>>(highScores.entrySet());
            entryList.sort(Entry.comparingByValue(Comparator.reverseOrder()));
            LinkedHashMap<String, Integer> sortedHighScores = new LinkedHashMap<String, Integer>();

            for (Entry<String, Integer> entry : entryList) {
                sortedHighScores.put(entry.getKey(), entry.getValue());
            }

            // Cull anything that isn't at least half the score of the highest result
            Iterator<Entry<String, Integer>> iter = sortedHighScores.entrySet().iterator();
            Integer highestScore = iter.next().getValue();

            while (iter.hasNext()) {
                Entry<String, Integer> entry = iter.next();
                Integer score = entry.getValue();

                if (score < Math.ceil(highestScore / 2)) {
                    iter.remove();
                }
            }

            StringSelectMenu.Builder selectMenu = StringSelectMenu.create("gameindex:select:" + event.getId() + ":" + event.getUser().getId());
            
            for (String serial : sortedHighScores.keySet()) {
                Map<String, Object> gameEntry = (Map<String, Object>) mainGameIndexMap.get(serial);
                String label = serial + " / ";

                if (gameEntry.containsKey("name-en") && preferEnglish) {
                    label += StringUtils.abbreviate((String) gameEntry.get("name-en"), 80);
                } else {
                    label += StringUtils.abbreviate(((String) gameEntry.get("name")), 80);
                }

                selectMenu.addOption(label, serial);
            }

            selectMenu.setPlaceholder("Select a game");
            event.getHook().editOriginal("Search results are below; select the entry which matches the desired game and region:")
                .setComponents(ActionRow.of(selectMenu.build()))
                .queue();
        }
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("gameindex", "Look up information stored in GameIndex.yaml (otherwise known as 'GameDB')")
                .addOption(OptionType.STRING, "search", "Serial number or name to search for (e.g. 'SLUS-12345' or 'my game name')", true)
                .addOption(OptionType.BOOLEAN, "prefer-english", "(Default false) Prefer English names for non-English results, when available", false)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }

    @Override 
    public void handleStringSelectEvent(StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        String[] parts = componentId.split(":");

        if (parts.length != 4) {
            Messaging.logInfo("CommandGameIndex", "handleStringSelectEvent", "Received a string select menu event, but got a malformed string select menu ID. Received:\n```\n" + componentId + "\n```");
            event.getHook().sendMessage("Malformed select menu, admins have been notified").setEphemeral(true).queue();
            return;
        }

        String userId = parts[3];

        if (!event.getUser().getId().equals(userId)) {
            event.getHook().sendMessage("You are not allowed to use someone else's select menu").setEphemeral(true).queue();
            return;
        }

        if (event.getSelectedOptions().size() != 1) {
            event.getHook().sendMessage("Illegal selection size").setEphemeral(true).queue();
            return;
        }

        SelectOption opt = event.getSelectedOptions().get(0);
        MessageEmbed embed = HifumiBot.getSelf().getGameIndex().present(opt.getValue());
        StringSelectMenu newSelectMenu = event.getSelectMenu().createCopy().setDefaultOptions(opt).build();
        event.getHook().editOriginal("Showing " + opt.getValue() + "; use the select menu again to view another.")
            .setEmbeds(embed)
            .setComponents(ActionRow.of(newSelectMenu))
            .queue();
    }
}
