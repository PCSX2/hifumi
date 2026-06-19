// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
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
