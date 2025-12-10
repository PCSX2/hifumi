package net.pcsx2.hifumi.command.slash;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import net.pcsx2.hifumi.charting.ChartGenerator;
import net.pcsx2.hifumi.command.AbstractSlashCommand;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class CommandChartGen extends AbstractSlashCommand {

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        OptionMapping typeOpt = event.getOption("type");
        OptionMapping timeUnitOpt = event.getOption("time-unit");
        OptionMapping startDateOpt = event.getOption("start-date");
        OptionMapping endDateOpt = event.getOption("end-date");
        
        if (typeOpt == null || timeUnitOpt == null) {
            event.reply("Missing required options").setEphemeral(true).queue();
            return;
        }

        OffsetDateTime startDate = OffsetDateTime.MIN;
        OffsetDateTime endDate = OffsetDateTime.MAX;

        try {
            if (startDateOpt != null) {
                startDate = OffsetDateTime.of(LocalDate.parse(startDateOpt.getAsString()), LocalTime.MIDNIGHT, ZoneOffset.UTC);
            }
        } catch (Exception e) {
            event.reply("Invalid start date " + startDateOpt.getAsString()).setEphemeral(true).queue();
            return;
        }
        
        try {
            if (endDateOpt != null) {
                endDate = OffsetDateTime.of(LocalDate.parse(endDateOpt.getAsString()), LocalTime.MIDNIGHT, ZoneOffset.UTC);
            }
        } catch (Exception e) {
            event.reply("Invalid end date " + endDateOpt.getAsString()).setEphemeral(true).queue();
            return;
        }

        FileUpload file = null;

        switch (typeOpt.getAsString()) {
            case "warez": {
                file = FileUpload.fromData(ChartGenerator.buildWarezChart(startDate.toEpochSecond(), endDate.toEpochSecond(), timeUnitOpt.getAsString()), "warez.png");
                break;
            }
            case "member": {
                file = FileUpload.fromData(ChartGenerator.buildMemberChart(startDate.toEpochSecond(), endDate.toEpochSecond(), timeUnitOpt.getAsString()), "member.png"); 
                break;
            }
            case "automod": {
                file = FileUpload.fromData(ChartGenerator.buildAutomodChart(startDate.toEpochSecond(), endDate.toEpochSecond(), timeUnitOpt.getAsString()), "auutomod.png"); 
                break;
            }
            default: {
                event.reply("Unknown chart type").setEphemeral(true).queue();
                return;
            }
        }

        MessageCreateBuilder mb = new MessageCreateBuilder();
        mb.addFiles(file);
        event.reply(mb.build()).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        OptionData typeOption = new OptionData(OptionType.STRING, "type", "Type of chart to generate", true);
        typeOption.addChoice("warez", "warez");
        typeOption.addChoice("member", "member");
        typeOption.addChoice("automod", "automod");

        OptionData timeUnit = new OptionData(OptionType.STRING, "time-unit", "Unit of time to display each data point as", true);
        timeUnit.addChoice("day", "day");
        timeUnit.addChoice("month", "month");
        timeUnit.addChoice("year", "year");

        OptionData startDate = new OptionData(OptionType.STRING, "start-date", "YYYY-MM-DD formatted date (inclusive)", false);
        OptionData endDate = new OptionData(OptionType.STRING, "end-date", "YYYY-MM-DD formatted date (exclusive)", false);

        return Commands.slash("chartgen", "Generate a chart")
                .addOptions(typeOption, timeUnit, startDate, endDate)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

}

