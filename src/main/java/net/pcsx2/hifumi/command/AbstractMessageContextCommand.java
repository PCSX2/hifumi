// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command;

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public abstract class AbstractMessageContextCommand {

    public void executeIfPermission(MessageContextInteractionEvent event) {
        onExecute(event);
    }
    
    protected abstract void onExecute(MessageContextInteractionEvent event);
    public void onButtonEvent(ButtonInteractionEvent event) { }
    public void onSelectionEvent(EntitySelectInteractionEvent event) { }
    protected abstract CommandData defineMessageContextCommand();
}
