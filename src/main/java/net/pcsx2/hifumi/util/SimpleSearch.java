// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi.util;

import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.text.similarity.JaccardSimilarity;

public class SimpleSearch {

    private static JaccardSimilarity jaccard = new JaccardSimilarity();

    public static synchronized HashMap<String, Float> search(Collection<String> searchAgainst, String query) {
        HashMap<String, Float> ret = new HashMap<String, Float>();
        query = query.toLowerCase().trim().replaceAll("[-_]", " ").replaceAll("[^\\w\\s]", "");

        for (String str : searchAgainst) {
            String normalized = str.toLowerCase().trim().replaceAll("[-_]", " ").replaceAll("[^\\w\\s]", "");

            float toPush = 0;

            String[] tokenizedQuery = query.split("\\s");

            for (String token : tokenizedQuery) {
                if (token.isBlank())
                    continue;

                // Contains
                if (normalized.contains(token))
                    toPush += 0.5;

                // Whole word match
                for (String part : normalized.replaceAll("[^\\d\\w]", " ").split(" ")) {
                    if (part.equals(token))
                        toPush += 1;
                }
            }

            toPush += jaccard.apply(normalized, query);

            if (toPush >= 0.5)
                ret.put(str, toPush);
        }

        return ret;
    }
}
