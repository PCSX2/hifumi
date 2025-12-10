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
import net.pcsx2.hifumi.command.AbstractSlashCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class CommandRun extends AbstractSlashCommand {
    
    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        String runnableName = event.getOption("runnable").getAsString();
        boolean result = HifumiBot.getSelf().getScheduler().runScheduledNow(runnableName);
        
        if (result) {
            event.reply("Sent an execute request for runnable '" + runnableName + "' to the thread pool; it will run whenever a thread is available to host it.").queue();
        } else {
            event.reply("Something went wrong while trying to add the runnable to the thread pool; try again in a few moments.").setEphemeral(true).queue();
        }
    }

    @Override
    protected CommandData defineSlashCommand() {
        OptionData runnable = new OptionData(OptionType.STRING, "runnable", "Name of the runnable to execute").setRequired(true);
        
        for (String runnableName : HifumiBot.getSelf().getScheduler().getRunnableNames()) {
            runnable.addChoice(runnableName, runnableName);
        }
        
        return Commands.slash("run", "Execute a scheduled runnable immediately")
                .addOptions(runnable)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

}
