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
