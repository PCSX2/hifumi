// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.dynamic;

import java.util.HashMap;

public class DynamicSubcommand {
    
    private String name;
    private String description;
    private HashMap<String, DynamicChoice> choices;
    
    public DynamicSubcommand(String name, String description) {
        this.name = name;
        this.description = description;
        this.choices = new HashMap<String, DynamicChoice>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public HashMap<String, DynamicChoice> getChoices() {
        return choices;
    }
    
    public DynamicChoice getChoice(String choiceName) {
        return choices.get(choiceName);
    }
    
    public void putChoice(DynamicChoice choice) {
        choices.put(choice.getName(), choice);
    }
    
    public void clearChoice(String choiceName) {
        choices.remove(choiceName);
    }
}
