package net.pcsx2.hifumi.config;

import java.util.ArrayList;

public class SettingsIniParserConfig implements IConfig {

    @Override
    public ConfigType getConfigType() {
        return ConfigType.SETTINGS_PARSER;
    }

    @Override
    public boolean usePrettyPrint() {
        return true;
    }

    public ArrayList<Section> sections;
    
    public SettingsIniParserConfig() {
        sections = new ArrayList<Section>();
    }

    public class Section {
        public String sectionName;
        public ArrayList<Setting> settings;

        public Section() {
            sectionName = new String("");
            settings = new ArrayList<Setting>();
        }
    }

    public class Setting {
        public String settingName;
        public ArrayList<Rule> rules;

        public Setting() {
            settingName = new String("");
            rules = new ArrayList<Rule>();
        }
    }
    
    public class Rule {
        public int settingsType; // 1 = global, 2 = game properties, 3 = both
        public ArrayList<String> expectedValues;
        public boolean invert;
        public String message;
        
        public Rule() {
            settingsType = 0;
            expectedValues = new ArrayList<String>();
            invert = false;
            message = new String("");
        }
    }
}