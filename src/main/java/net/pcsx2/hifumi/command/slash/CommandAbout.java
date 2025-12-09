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

import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.Scheduler.NoSuchRunnableException;
import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.config.ConfigManager;
import net.pcsx2.hifumi.config.ConfigType;
import net.pcsx2.hifumi.util.Messaging;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandAbout extends AbstractSlashCommand {
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("About " + HifumiBot.getSelf().getJDA().getSelfUser().getName());
        eb.setDescription("A helper bot created for the PCSX2 Discord server.");
        eb.addField("Created By", "pandubz", true);
        String version = HifumiBot.getSelf().getVersion();
        eb.addField("Version", version != null ? version : "[Debug Mode]", true);
        
        long total = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();

        eb.addField("Memory (Total / Free / Used)", String.format("%d MB / %d MB / %d MB", total / 1024 / 1024, free / 1024 / 1024, (total - free) / 1024 / 1024), false);

        StringBuilder storageBuilder = new StringBuilder("| ");
        storageBuilder.append("Config: ").append((ConfigManager.getSizeBytes(ConfigType.CORE) / 1024) + " KB | ");
        storageBuilder.append("DynCmd: ").append((ConfigManager.getSizeBytes(ConfigType.DYNCMD) / 1024) + " KB | ");
        storageBuilder.append("Emulog: ").append((ConfigManager.getSizeBytes(ConfigType.EMULOG_PARSER) / 1024) + " KB | ");
        eb.addField("Storage Size", storageBuilder.toString(), false);
        StringBuilder runnableBuilder = new StringBuilder("| ");

        for (String runnableName : HifumiBot.getSelf().getScheduler().getRunnableNames()) {
            try {
                runnableBuilder.append(runnableName + ": "
                        + (HifumiBot.getSelf().getScheduler().isRunnableAlive(runnableName) ? "alive" : "stopped")
                        + " | ");
            } catch (NoSuchRunnableException e) {
                Messaging.logException("CommandAbout", "onExecute", e);
            }
        }

        eb.addField("Runnable Statuses", runnableBuilder.toString().trim(), false);
        event.replyEmbeds(eb.build()).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        return Commands.slash("about", "View general information about the bot and its health")
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }
}
