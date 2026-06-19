// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.config;

import java.util.ArrayList;

public class EmulogParserConfig implements IConfig {

    @Override
    public ConfigType getConfigType() {
        return ConfigType.EMULOG_PARSER;
    }

    @Override
    public boolean usePrettyPrint() {
        return true;
    }

    public ArrayList<Rule> rules;
    
    public EmulogParserConfig() {
        rules = new ArrayList<Rule>();
    }
    
    public class Rule {
        public String name;
        public String toMatch;
        public String message;
        public int severity;
        
        public Rule() {
            name = new String("");
            toMatch = new String("");
            message = new String("");
            severity = 0;
        }
    }
}
