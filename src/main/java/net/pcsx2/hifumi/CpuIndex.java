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
package net.pcsx2.hifumi;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.Refreshable;

public class CpuIndex implements Refreshable {

    public static final String PASSMARK_STR_URL = "https://www.cpubenchmark.net/singleThread.html";

    private boolean isInitialized = false;
    private ConcurrentHashMap<String, String> cpuMap = new ConcurrentHashMap<String, String>();

    public CpuIndex() {
        
    }

    public synchronized void refresh() {
        try {
            Document doc = Jsoup.connect(PASSMARK_STR_URL).maxBodySize(0).get();
            Elements charts = doc.getElementsByClass("chartlist");

            if (charts.size() > 0) {
                this.clear();

                for (Element chart : charts) {
                    Elements rows = chart.getElementsByTag("li");

                    for (Element row : rows) {
                        String cpuName = row.getElementsByClass("prdname").get(0).text();
                        String rating = row.getElementsByClass("count").get(0).text();
                        this.addCpu(cpuName, rating);
                    }
                }
            }

            this.isInitialized = true;
        } catch (IOException e) {
            Messaging.logException("CpuIndex", "refresh", e);
        }
    }

    public synchronized boolean isInitialized() {
        return this.isInitialized;
    }

    public synchronized void clear() {
        cpuMap.clear();
    }

    public synchronized void addCpu(String name, String rating) {
        cpuMap.put(name, rating);
    }

    public synchronized Set<String> getAllCpus() {
        return cpuMap.keySet();
    }

    public synchronized String getCpuRating(String name) {
        return cpuMap.get(name);
    }
}
