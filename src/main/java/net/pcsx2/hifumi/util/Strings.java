// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Strings {

    private static final Pattern URL_PATTERN = Pattern.compile("((?:https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])");

    public static String unescapeNewlines(String input) {
        return input.replace("\\n", "\n");
    }

    public static ArrayList<String> extractUrls(String input) {
        ArrayList<String> urls = new ArrayList<String>();
        Matcher m = URL_PATTERN.matcher(input);
        
        while (m.find()) {
            String url = m.group();

            if (url != null) {
                urls.add(url);
            }
        }

        return urls;
    }

    public static String getEnvVarOrPanic(String envVar) {
        var value = System.getenv().getOrDefault(envVar, null);
        if (value == null) {
            throw new RuntimeException(String.format("Did not provide env-var: '%s'", envVar));
        }
        return value;
    }
}
