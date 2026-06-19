// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.command;

import java.util.HashMap;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.pcsx2.hifumi.HifumiBot;
import net.pcsx2.hifumi.command.context.CommandBan;
import net.pcsx2.hifumi.command.context.CommandReverseImage;
import net.pcsx2.hifumi.command.context.CommandTranslateEN;
import net.pcsx2.hifumi.command.context.CommandWarezMsg;
import net.pcsx2.hifumi.command.dynamic.DynamicChoice;
import net.pcsx2.hifumi.command.dynamic.DynamicCommand;
import net.pcsx2.hifumi.command.dynamic.DynamicSubcommand;
import net.pcsx2.hifumi.command.slash.CommandAbout;
import net.pcsx2.hifumi.command.slash.CommandBulkDelete;
import net.pcsx2.hifumi.command.slash.CommandCPU;
import net.pcsx2.hifumi.command.slash.CommandChartGen;
import net.pcsx2.hifumi.command.slash.CommandConfig;
import net.pcsx2.hifumi.command.slash.CommandDynCmd;
import net.pcsx2.hifumi.command.slash.CommandEmulog;
import net.pcsx2.hifumi.command.slash.CommandGPU;
import net.pcsx2.hifumi.command.slash.CommandGameIndex;
import net.pcsx2.hifumi.command.slash.CommandPFP;
import net.pcsx2.hifumi.command.slash.CommandPerms;
import net.pcsx2.hifumi.command.slash.CommandPride;
import net.pcsx2.hifumi.command.slash.CommandPrompt;
import net.pcsx2.hifumi.command.slash.CommandReload;
import net.pcsx2.hifumi.command.slash.CommandRun;
import net.pcsx2.hifumi.command.slash.CommandSay;
import net.pcsx2.hifumi.command.slash.CommandScamHash;
import net.pcsx2.hifumi.command.slash.CommandSerial;
import net.pcsx2.hifumi.command.slash.CommandServerMetadata;
import net.pcsx2.hifumi.command.slash.CommandShutdown;
import net.pcsx2.hifumi.command.slash.CommandSpamKick;
import net.pcsx2.hifumi.command.slash.CommandTranslate;
import net.pcsx2.hifumi.command.slash.CommandUnwarez;
import net.pcsx2.hifumi.command.slash.CommandWarez;
import net.pcsx2.hifumi.command.slash.CommandWarezHistory;
import net.pcsx2.hifumi.command.slash.CommandWhois;
import net.pcsx2.hifumi.util.Messaging;

public class CommandIndex {

    private CommandListUpdateAction commandsToRegister;
    private HashMap<String, AbstractSlashCommand> slashCommands;
    private HashMap<String, AbstractMessageContextCommand> messageCommands;
    private HashMap<String, AbstractUserContextCommand> userCommands;

    /**
     * Create a new CommandIndex and invoke the {@link CommandIndex#rebuild
     * rebuild()} method.
     */
    public CommandIndex() {
        slashCommands = new HashMap<String, AbstractSlashCommand>();
        messageCommands = new HashMap<String, AbstractMessageContextCommand>();
        userCommands = new HashMap<String, AbstractUserContextCommand>();
        rebuild();
    }
    
    /**
     * Rebuild this CommandIndex from the Config object in HifumiBot.
     */
    public void rebuild() {
        commandsToRegister = HifumiBot.getSelf().getJDA().updateCommands();
        rebuildSlash();
        rebuildMessage();
        rebuildUser();
        rebuildDynamic();
        commandsToRegister.queue();
    }
    
    public void rebuildSlash() {
        slashCommands.clear();
        registerSlashCommand(new CommandSay());
        registerSlashCommand(new CommandAbout());
        registerSlashCommand(new CommandWarez());
        registerSlashCommand(new CommandShutdown());
        registerSlashCommand(new CommandReload());
        registerSlashCommand(new CommandRun());
        registerSlashCommand(new CommandPFP());
        registerSlashCommand(new CommandPerms());
        registerSlashCommand(new CommandCPU());
        registerSlashCommand(new CommandGPU());
        registerSlashCommand(new CommandDynCmd());
        registerSlashCommand(new CommandSpamKick());
        registerSlashCommand(new CommandGameIndex());
        registerSlashCommand(new CommandEmulog());
        registerSlashCommand(new CommandTranslate());
        registerSlashCommand(new CommandBulkDelete());
        registerSlashCommand(new CommandSerial());
        registerSlashCommand(new CommandPrompt());
        registerSlashCommand(new CommandChartGen());
        registerSlashCommand(new CommandPride());
        registerSlashCommand(new CommandWarezHistory());
        registerSlashCommand(new CommandWhois());
        registerSlashCommand(new CommandServerMetadata());
        registerSlashCommand(new CommandUnwarez());
        registerSlashCommand(new CommandConfig());
        registerSlashCommand(new CommandScamHash());
    }
    
    public void rebuildMessage() {
        messageCommands.clear();
        registerMessageCommand(new CommandTranslateEN());
        registerMessageCommand(new CommandReverseImage());
        registerMessageCommand(new CommandWarezMsg());
    }

    public void rebuildUser() {
        userCommands.clear();
        registerUserCommand(new CommandBan());
    }
    
    public void rebuildDynamic() {
        HashMap<String, DynamicCommand> commands = HifumiBot.getSelf().getDynCmdConfig().dynamicCommands;
        int commandCount = 0;
        
        for (String commandName : commands.keySet()) {
            if (slashCommands.containsKey(commandName)) {
                Messaging.logInfo("CommandIndex", "rebuildDynamic", "Skipping dynamic command \"" + commandName + "\", found a built-in command with the same name");
                continue;
            }

            if (++commandCount > 100) {
                Messaging.logInfo("CommandIndex", "rebuildDynamic", "Command `/" + commandName + "` is being skipped, the limit of 100 commands has been reached.");
                continue;
            }
            
            DynamicCommand command = commands.get(commandName);
            SlashCommandData commandData = Commands.slash(command.getName(), command.getDescription());
            HashMap<String, DynamicSubcommand> subcommands = command.getSubcommands();
            int subcommandCount = 0;
            
            for (String subcommandName : subcommands.keySet()) {
                if (++subcommandCount > 25) {
                    Messaging.logInfo("CommandIndex", "rebuildDynamic", "Subcommand `/" + commandName + " " + subcommandName + "` is being skipped, the limit of 25 subcommands has been reached.");
                    continue;
                }

                DynamicSubcommand subcommand = subcommands.get(subcommandName);
                SubcommandGroupData subgroup = new SubcommandGroupData(subcommand.getName(), subcommand.getDescription());
                HashMap<String, DynamicChoice> choices = subcommand.getChoices();
                int choiceCount = 0;
                
                for (String choiceName : choices.keySet()) {
                    if (++choiceCount > 25) {
                        Messaging.logInfo("CommandIndex", "rebuildDynamic", "Choice `/" + commandName + " " + subcommandName + " " + choiceName + "` is being skipped, the limit of 25 choices has been reached.");
                        continue;
                    }

                    DynamicChoice choice = choices.get(choiceName);
                    SubcommandData subcommandData = new SubcommandData(choice.getName(), choice.getDescription());
                    subcommandData.addOption(OptionType.USER, "mention", "Mention");
                    subgroup.addSubcommands(subcommandData);
                }

                commandData.addSubcommandGroups(subgroup);
            }
            
            commandsToRegister.addCommands(commandData);
        }
    }
    
    private void registerSlashCommand(AbstractSlashCommand slashCommand) {
        String name = slashCommand.defineSlashCommand().getName();
        slashCommands.put(name, slashCommand);
        commandsToRegister.addCommands(slashCommand.defineSlashCommand());
    }
    
    private void registerMessageCommand(AbstractMessageContextCommand messageCommand) {
        String name = messageCommand.defineMessageContextCommand().getName();
        messageCommands.put(name, messageCommand);
        commandsToRegister.addCommands(messageCommand.defineMessageContextCommand());
    }

    private void registerUserCommand(AbstractUserContextCommand userCommand) {
        String name = userCommand.defineUserContextCommand().getName();
        userCommands.put(name, userCommand);
        commandsToRegister.addCommands(userCommand.defineUserContextCommand());
    }
    
    public HashMap<String, AbstractSlashCommand> getSlashCommands() {
        return slashCommands;
    }
    
    public HashMap<String, AbstractMessageContextCommand> getMessageCommands() {
        return messageCommands;
    }

    public HashMap<String, AbstractUserContextCommand> getUserCommands() {
        return userCommands;
    }
}
