/**
 * This file is part of HifumiBot, licensed under the MIT License (MIT)
 * 
 * Copyright (c) 2020 RedPanda4552 (https://github.com/RedPanda4552)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
