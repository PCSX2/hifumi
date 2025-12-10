package net.pcsx2.hifumi;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class BrowsableEmbed {

    public static ConcurrentHashMap<Long, BrowsableEmbed> embedCache = new ConcurrentHashMap<Long, BrowsableEmbed>();
    
    private String sourceSlug;
    private long eventIdLong;
    private long userIdLong;
    private long createdTimestamp;
    private ArrayList<MessageEmbed> pages;
    private int currentPage;
    
    public BrowsableEmbed(String sourceSlug, long eventIdLong, long userIdLong, ArrayList<MessageEmbed> embeds) {
        this.sourceSlug = sourceSlug;
        this.eventIdLong = eventIdLong;
        this.userIdLong = userIdLong;
        this.createdTimestamp = OffsetDateTime.now().toEpochSecond();
        this.pages = new ArrayList<MessageEmbed>();

        for (MessageEmbed embed : embeds) {
            this.pages.add(embed);
        }

        this.currentPage = (this.pages.isEmpty() ? -1 : 0);
    }

    public long getEventIdLong() {
        return this.eventIdLong;
    }

    public long getUserIdLong() {
        return this.userIdLong;
    }

    public long getCreatedTimestamp() {
        return this.createdTimestamp;
    }

    /**
     * Get the current page. Starts at position 0.
     * @return Optional containing a MessageEmbed, if a page exists.
     */
    public Optional<MessageEmbed> getCurrentPage() {
        if (this.pages.isEmpty() || this.currentPage < 0) {
            return Optional.empty();
        }

        return Optional.of(this.pages.get(this.currentPage));
    }

    /**
     * Fetch a copy of the next page without advancing to it.
     * @return The next page, if one exists.
     */
    public Optional<MessageEmbed> previewNextPage() {
        if (this.currentPage + 1 >= this.pages.size()) {
            return Optional.empty();
        }

        return Optional.of(this.pages.get(this.currentPage + 1));
    }

    /**
     * Advance to and return a copy of the next page.
     * @return The next page, or if no further page exists, the same page.
     */
    public Optional<MessageEmbed> nextPage() {
        if (++this.currentPage >= this.pages.size()) {
            --this.currentPage;
        }

        return this.getCurrentPage();
    }

    /**
     * Fetch a copy of the previous page without advancing to it.
     * @return The previous page, if one exists.
     */
    public Optional<MessageEmbed> previewPreviousPage() {
        if (this.currentPage - 1 < 0) {
            return Optional.empty();
        }

        return Optional.of(this.pages.get(this.currentPage - 1));
    }

    /**
     * Back up to and return a copy of the previous page.
     * @return The previous page, or if at the first page, the same page.
     */
    public Optional<MessageEmbed> prevPage() {
        if (--this.currentPage < 0) {
            ++this.currentPage;
        }

        return this.getCurrentPage();
    }

    public ArrayList<Button> refreshButtonOptions() {
        ArrayList<Button> buttons = new ArrayList<Button>();
        Optional<MessageEmbed> prevPreview = this.previewPreviousPage();
        Optional<MessageEmbed> nextPreview = this.previewNextPage();
        
        if (prevPreview.isPresent()) {
            buttons.add(
                Button.of(ButtonStyle.SECONDARY, sourceSlug + ":prev:" + this.getEventIdLong() + ":" + this.getUserIdLong(), prevPreview.get().getTitle())
            );
        } else {
            buttons.add(
                Button.of(ButtonStyle.SECONDARY, sourceSlug + ":prev:" + this.getEventIdLong() + ":" + this.getUserIdLong(), "(no previous page)").asDisabled()
            );
        }

        if (nextPreview.isPresent()) {
            buttons.add(
                Button.of(ButtonStyle.PRIMARY, sourceSlug + ":next:" + this.getEventIdLong() + ":" + this.getUserIdLong(), nextPreview.get().getTitle())
            );
        } else {
            buttons.add(
                Button.of(ButtonStyle.PRIMARY, sourceSlug + ":next:" + this.getEventIdLong() + ":" + this.getUserIdLong(), "(no next page)").asDisabled()
            );
        }

        return buttons;
    }
}
