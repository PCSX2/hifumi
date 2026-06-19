// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.config;

public enum ConfigType {

    CORE("hifumi-conf.json", Config.class),
    DYNCMD("dyncmd-config.json", DynCmdConfig.class),
    EMULOG_PARSER("emulog-parser.json", EmulogParserConfig.class),
    SETTINGS_PARSER("settings-ini-parser.json", SettingsIniParserConfig.class);
    
    private String fileName;
    private Class<?> clazz;
    
    private ConfigType(String fileName, Class<?> clazz) {
        this.fileName = fileName;
        this.clazz = clazz;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public Class<?> getConfigClass() {
        return clazz;
    }
}
