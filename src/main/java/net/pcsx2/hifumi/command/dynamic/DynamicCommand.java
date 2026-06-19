// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command.dynamic;

import java.util.HashMap;

public class DynamicCommand {
    
    private String name;
    private String description;
    private HashMap<String, DynamicSubcommand> subcommands;
    
    public DynamicCommand(String name, String description) {
        this.name = name;
        this.description = description;
        this.subcommands = new HashMap<String, DynamicSubcommand>();
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public HashMap<String, DynamicSubcommand> getSubcommands() {
        return subcommands;
    }
    
    public DynamicSubcommand getSubcommand(String subcommandName) {
        return subcommands.get(subcommandName);
    }
    
    public void putSubcommand(DynamicSubcommand subcommand) {
        subcommands.put(subcommand.getName(), subcommand);
    }
    
    public void clearSubcommand(String subcommandName) {
        subcommands.remove(subcommandName);
    }
}
