// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.config;

public interface IConfig {
    
    public ConfigType getConfigType();
    public boolean usePrettyPrint();
}
