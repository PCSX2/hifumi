// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.slash;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import net.pcsx2.hifumi.GpuIndex;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.permissions.PermissionLevel;
import net.pcsx2.hifumi.util.SimpleSearch;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandGPU extends AbstractSlashCommand {
    
    private enum GPURating {    
        x8NATIVE("8x Native (~5K)", 13030), 
        x6NATIVE("6x Native (~4K)", 8660), 
        x5NATIVE("5x Native (~3K)", 6700),
        x4NATIVE("4x Native (~2K)", 4890), 
        x3NATIVE("3x Native (~1080p)", 3230), 
        x2NATIVE("2x Native (~720p)", 1720),
        NATIVE("Native", 360), 
        SLOW("Slow", 0);

        private String displayName;
        private int minimum;

        private GPURating(String displayName, int minimum) {
            this.displayName = displayName;
            this.minimum = minimum;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getMinimum() {
            return minimum;
        }
    }

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        boolean isEphemeral = true;
        
        if (event.getChannel().getId().equals(HifumiBot.getSelf().getConfig().channels.restrictedCommandChannelId) || HifumiBot.getSelf().getPermissionManager().hasPermission(PermissionLevel.MOD, event.getMember())) {
            isEphemeral = false;
        }
        
        EmbedBuilder eb = new EmbedBuilder();
        OptionMapping opt = event.getOption("name");
        
        if (opt == null) {
            event.reply("Missing required argument `name`").setEphemeral(isEphemeral).queue();
            return;
        }
        
        String name = opt.getAsString();
        GpuIndex gpuIndex = HifumiBot.getSelf().getGpuIndex();

        if (!gpuIndex.isInitialized()) {
            event.reply("Whoa there! The bot is still fetching data from Passmark, please wait a moment while that finishes!").queue();
            return;
        }

        HashMap<String, Float> results = SimpleSearch.search(gpuIndex.getAllGpus(), StringUtils.join(name, " "));
        
        if (results.size() > 0) {
            eb.setAuthor("Passmark GPU Performance", "https://www.videocardbenchmark.net/");
            eb.setTitle("Search results for '" + StringUtils.join(name, " ").trim() + "'");
            eb.setDescription(":warning: Some games may have unusually high GPU requirements! If in doubt, ask!");
            String highestName = null;
            float highestWeight = 0;

            while (!results.isEmpty() && eb.getFields().size() < 5) {
                for (String gpuName : results.keySet()) {
                    if (results.get(gpuName) > highestWeight) {
                        highestName = gpuName;
                        highestWeight = results.get(gpuName);
                    }
                }

                results.remove(highestName);
                highestWeight = 0;
                int highestScore = -1;

                try {
                    highestScore = Integer.parseInt(gpuIndex.getGpuRating(highestName).replaceAll("[,. ]", ""));
                } catch (NumberFormatException e) { }

                String highestScoreDescription = "";

                for (int i = 0; i < GPURating.values().length; i++) {
                    if (highestScore >= GPURating.values()[i].getMinimum()) {
                        highestScoreDescription = GPURating.values()[i].getDisplayName();
                        break;
                    }
                }

                eb.addField(highestName, highestScore + " - " + highestScoreDescription, false);
            }

            eb.setColor(0x00ff00);
        } else {
            eb.setTitle("No results matched your query!");
            eb.setColor(0xff0000);
        }
        
        event.replyEmbeds(eb.build()).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("gpu", "Look up the rating of a GPU")
                .addOption(OptionType.STRING, "name", "Name of the GPU to look up", true)
                .setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }
}
