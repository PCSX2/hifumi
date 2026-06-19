// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.config;

import java.util.HashMap;

import net.pcsx2.hifumi.command.dynamic.DynamicCommand;

public class DynCmdConfig implements IConfig {
    
    @Override
    public ConfigType getConfigType() {
        return ConfigType.DYNCMD;
    }
    
    @Override
    public boolean usePrettyPrint() {
        return true;
    }
    
    public HashMap<String, DynamicCommand> dynamicCommands;

    public DynCmdConfig() {
        dynamicCommands = new HashMap<String, DynamicCommand>();
    }
}
