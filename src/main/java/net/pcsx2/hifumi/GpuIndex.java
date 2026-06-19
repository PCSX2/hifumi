// SPDX-FileCopyrightText: 2026 PCSX2 Dev Team
// SPDX-License-Identifier: MIT
package net.pcsx2.hifumi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.Refreshable;

public class GpuIndex implements Refreshable {

    public static final String PASSMARK_HIGH_END = "https://www.videocardbenchmark.net/high_end_gpus.html";
    public static final String PASSMARK_MID_HIGH = "https://www.videocardbenchmark.net/mid_range_gpus.html";
    public static final String PASSMARK_MID_LOW = "https://www.videocardbenchmark.net/midlow_range_gpus.html";
    public static final String PASSMARK_LOW_END = "https://www.videocardbenchmark.net/low_end_gpus.html";

    private boolean isInitialized = false;
    private ConcurrentHashMap<String, String> gpuMap = new ConcurrentHashMap<String, String>();

    public GpuIndex() {
        
    }

    public synchronized void refresh() {
        HashMap<String, String> highEnd = this.refresh(PASSMARK_HIGH_END);
        HashMap<String, String> midHigh = this.refresh(PASSMARK_MID_HIGH);
        HashMap<String, String> midLow = this.refresh(PASSMARK_MID_LOW);
        HashMap<String, String> lowEnd = this.refresh(PASSMARK_LOW_END);
        this.clear();
        gpuMap.putAll(highEnd);
        gpuMap.putAll(midHigh);
        gpuMap.putAll(midLow);
        gpuMap.putAll(lowEnd);
    }

    private HashMap<String, String> refresh(String url) {
        HashMap<String, String> ret = new HashMap<String, String>();

        try {
            Document doc = Jsoup.connect(url).maxBodySize(0).get();
            Element mark = doc.getElementById("mark");
            Elements charts = mark.getElementsByClass("chartlist");

            for (Element chart : charts) {
                Elements rows = chart.getElementsByTag("li");

                for (Element row : rows) {
                    String gpuName = row.getElementsByClass("prdname").get(0).text();
                    String rating = row.getElementsByClass("count").get(0).text();
                    ret.put(gpuName, rating);
                }
            }

            this.isInitialized = true;
        } catch (IOException e) {
            Messaging.logException("GpuIndex", "refresh", e);
        }

        return ret;
    }

    public synchronized boolean isInitialized() {
        return this.isInitialized;
    }

    public synchronized void clear() {
        gpuMap.clear();
    }

    public synchronized void addGpu(String name, String rating) {
        gpuMap.put(name, rating);
    }

    public synchronized Set<String> getAllGpus() {
        return gpuMap.keySet();
    }

    public synchronized String getGpuRating(String name) {
        return gpuMap.get(name);
    }
}
