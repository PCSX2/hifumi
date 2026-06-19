// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class AbstractSlashCommand {
    
    public AbstractSlashCommand() {
    }
    
    public abstract void onExecute(SlashCommandInteractionEvent event);
    public void handleButtonEvent(ButtonInteractionEvent event) { }
    public void handleStringSelectEvent(StringSelectInteractionEvent event) { }
    protected abstract CommandData defineSlashCommand();
}
