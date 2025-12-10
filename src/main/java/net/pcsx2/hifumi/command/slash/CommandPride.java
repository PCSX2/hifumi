package net.pcsx2.hifumi.command.slash;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import net.pcsx2.hifumi.command.AbstractSlashCommand;
import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.database.objects.CounterObject;
import net.pcsx2.hifumi.util.DateTimeUtils;
import net.pcsx2.hifumi.util.EmbedUtil;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class CommandPride extends AbstractSlashCommand {

    @Override
    public void onExecute(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = EmbedUtil.newFootedEmbedBuilder(event.getUser());

        CounterObject prideCounter = Database.getLatestCounter("pride");
        OffsetDateTime currentPrideTime = DateTimeUtils.longToOffsetDateTime(prideCounter.getValue());

        switch (event.getSubcommandName()) {
            case "add": {
                currentPrideTime = currentPrideTime.plus(1, ChronoUnit.MONTHS);
                Database.insertCounter("pride", OffsetDateTime.now().toEpochSecond(), currentPrideTime.toEpochSecond());
                
                eb.setTitle("Add one to the count!");
                eb.setDescription("""
                    Our pride logo is sticking around until no one complains about it anymore.\n
                    For each complaint, we will extend the time we keep our pride logo by one month\n
                    If you're reading this that means someone just bumped the time!
                """);
                break;
            }
            case "get": {
                eb.setTitle("The pride logo removal ETA");
                eb.setDescription("""
                    Every time someone complains about the server logo, we extend its life for an extra month past the end of pride month.\n\n
                """);
                break;
            }
        }

        String dateStr = currentPrideTime.format(DateTimeFormatter.ofPattern("MMM dd yyyy"));
        eb.addField("Current ETA to remove pride logo:", dateStr, false);
        event.replyEmbeds(eb.build()).queue();
    }

    @Override
    protected CommandData defineSlashCommand() {
        SubcommandData add = new SubcommandData("add", "Add a month to the ETA");
        SubcommandData get = new SubcommandData("get", "Get the current ETA");
        return Commands.slash("pride", "For as long as there is the logo, there shall be carnage")
                .addSubcommands(add, get)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_SERVER));
    }

}
