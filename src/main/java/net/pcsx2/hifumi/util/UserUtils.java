package net.pcsx2.hifumi.util;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import net.pcsx2.hifumi.HifumiBot;

import net.dv8tion.jda.api.entities.User;

public class UserUtils {

    public static Optional<User> getOrRetrieveUser(long userIdLong) {
        return UserUtils.getOrRetrieveUser(String.valueOf(userIdLong));
    }

    public static Optional<User> getOrRetrieveUser(String userId) {
        try {
            User usr = null;

            if ((usr = HifumiBot.getSelf().getJDA().getUserById(userId)) != null
                    || (usr = HifumiBot.getSelf().getJDA().retrieveUserById(userId).complete()) != null) {
                return Optional.of(usr);
            }
        } catch (Exception e) {
            // Squelch
        }

        return Optional.empty();
    }

    public static String getAgeOfUserAsPrettyString(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        Duration diff = Duration.between(user.getTimeCreated(), now);
        String ageStr = "";

        if (diff.toSeconds() < 60) {
            ageStr = diff.toSeconds() + "s";
        } else if (diff.toMinutes() < 60) {
            ageStr = diff.toMinutes() + "m " + diff.toSecondsPart() + "s";
        } else if (diff.toHours() < 24) {
            ageStr = diff.toHours() + "h " + diff.toMinutesPart() + "m";
        } else {
            ageStr = diff.toDays() + "d " + diff.toHoursPart() + "h";
        }

        return ageStr;
    }
}
