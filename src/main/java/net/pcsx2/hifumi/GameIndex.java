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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;

import net.pcsx2.hifumi.util.EmbedUtil;
import net.pcsx2.hifumi.util.Messaging;
import net.pcsx2.hifumi.util.Refreshable;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import okhttp3.Request;
import okhttp3.Response;

public class GameIndex implements Refreshable {

    private static final String GAMEINDEX_LOCATION = "https://raw.githubusercontent.com/PCSX2/pcsx2/master/bin/resources/GameIndex.yaml";
    
    private static final HashMap<Integer, String> COMPAT = new HashMap<Integer, String>() {{
        put(0, "Unknown");
        put(1, "Nothing");
        put(2, "Intro");
        put(3, "Menu");
        put(4, "In-game");
        put(5, "Playable");
        put(6, "Perfect");
    }};
    
    private static final HashMap<Integer, String> ROUNDING = new HashMap<Integer, String>() {{
        put(0, "Nearest");
        put(1, "Negative Infinity");
        put(2, "Positive Infinity");
        put(3, "Chop (Zero)");
    }};
    
    private static final HashMap<Integer, String> EE_CLAMPING = new HashMap<Integer, String>() {{
        put(0, "Disable");
        put(1, "Normal");
        put(2, "Extra + Preserve Sign");
        put(3, "Full Clamping");
    }};
    
    private static final HashMap<Integer, String> VU_CLAMPING = new HashMap<Integer, String>() {{
        put(0, "Disable");
        put(1, "Normal");
        put(2, "Extra");
        put(3, "Extra + Preserve Sign");
    }};
    
    private static final HashMap<Integer, String> MIPMAP = new HashMap<Integer, String>() {{
        put(0, "Off");
        put(1, "Basic");
        put(2, "Full");
    }};
    
    private static final HashMap<Integer, String> TRILINEAR = new HashMap<Integer, String>() {{
        put(0, "None");
        put(1, "Trilinear");
        put(2, "Trilinear Ultra");
    }};
    
    private static final HashMap<Integer, String> TEXTURE_PRELOADING = new HashMap<Integer, String>() {{
        put(0, "None");
        put(1, "Partial");
        put(2, "Full (Hash Cache)");
    }};
    
    private static final HashMap<Integer, String> DEINTERLACING = new HashMap<Integer, String>() {{
        put(0, "Automatic");
        put(1, "None");
        put(2, "Weave (TFF)");
        put(3, "Weave (BFF)");
        put(4, "Bob (TFF)");
        put(5, "Bob (BFF)");
        put(6, "Blend (TFF)");
        put(7, "Blend (BFF)");
        put(8, "Adaptive (TFF)");
        put(9, "Adaptive (BFF)");
    }};
    
    private static final HashMap<Integer, String> GENERIC_BOOLEAN = new HashMap<Integer, String>() {{
        put(0, "Off");
        put(1, "On");
    }};
    
    private static final HashMap<Integer, String> HALF_PIXEL_OFFSET = new HashMap<Integer, String>() {{
        put(0, "Off");
        put(1, "Normal (Vertex)");
        put(2, "Special (Texture)");
        put(3, "Special (Texture - Aggressive)");
        put(4, "Align to Native");
        put(5, "Align to Native with Texture Offset");
    }};
    
    private static final HashMap<Integer, String> ROUND_SPRITE = new HashMap<Integer, String>() {{
        put(0, "Off");
        put(1, "Half");
        put(2, "Full");
    }};
    
    private static final HashMap<Integer, String> HALF_BOTTOM = new HashMap<Integer, String>() {{
        put(0, "Force Disabled");
        put(1, "Force Enabled");
    }};
    
    private static final HashMap<Integer, String> GPU_PALETTE_CONVERSION = new HashMap<Integer, String>() {{
        put(0, "Off");
        put(1, "On");
        put(2, "On + Restrict paltex");
    }};

    private static final HashMap<Integer, String> BLENDING_ACCURACY = new HashMap<Integer, String>() {{
        put(0, "Minimum");
        put(1, "Basic");
        put(2, "Medium");
        put(3, "High");
        put(4, "Full");
        put(5, "Maximum");
    }};

    private static final HashMap<Integer, String> GPU_TARGET_CLUT = new HashMap<Integer, String>() {{
        put(0, "Disabled");
        put(1, "Enabled (Exact Match)");
        put(2, "Enabled (Check Inside Target)");
    }};

    private static final HashMap<Integer, String> AUTO_FLUSH = new HashMap<Integer, String>() {{
        put(0, "Off");
        put(1, "Sprites Only");
        put(2, "All Primitives");
    }};

    private static final HashMap<Integer, String> CPU_SPRITE_RENDER_LEVEL = new HashMap<Integer, String>() {{
        put(0, "Sprites Only");
        put(1, "Sprites/Triangles");
        put(2, "Blended Sprites/Triangles");
    }};

    private static final HashMap<Integer, String> EE_CYCLE_RATE = new HashMap<Integer, String>() {{
        put(-3, "50%");
        put(-2, "60%");
        put(-1, "75%");
        put(0, "100%");
        put(1, "130%");
        put(2, "180%");
        put(3, "300%");
    }};

    private static final HashMap<Integer, String> NATIVE_SCALING = new HashMap<Integer, String>() {{
        put(0, "Off");
        put(1, "Normal");
        put(2, "Aggressive");
        put(3, "Normal (Maintain Upscale)");
        put(4, "Aggressive (Maintain Upscale)");
    }};
    
    private boolean isInitialized = false;
    private Map<String, Object> map;
    
    public GameIndex() {
        
    }
    
    @Override
    public void refresh() {
        Request req = new Request.Builder().url(GAMEINDEX_LOCATION).get().build();
        
        try {
            Response res = HifumiBot.getSelf().getHttpClient().newCall(req).execute();
            
            if (res.isSuccessful()) {
                Yaml yaml = new Yaml();
                map = yaml.load(res.body().charStream());
            }

            this.isInitialized = true;
        } catch (IOException e) {
            Messaging.logException("GameIndex", "refresh", e);
        }
    }

    public boolean isInitialized() {
        return this.isInitialized;
    }

    public Map<String, Object> getMap() {
        return map;
    }
    
    @SuppressWarnings("unchecked")
    public MessageEmbed present(String serial) {
        EmbedBuilder eb = new EmbedBuilder();
        boolean entryFound = false;
        
        for (String key : map.keySet()) {
            if (key.equals(serial.toUpperCase())) {
                entryFound = true;
                Map<String, Object> entry = (Map<String, Object>) map.get(key);
                
                eb.setTitle((String) entry.get("name"));

                if (entry.containsKey("name-sort")) {
                    eb.appendDescription("Sorting Name: " + (String) entry.get("name-sort") + "\n");
                }

                if (entry.containsKey("name-en")) {
                    eb.appendDescription("English Name: " + (String) entry.get("name-en") + "\n");
                }
                
                if (entry.containsKey("region")) {
                    eb.appendDescription("Region: " + (String) entry.get("region") + "\n");
                }
                
                if (entry.containsKey("compat")) {
                    eb.appendDescription("Compatibility: " + COMPAT.get((int) entry.get("compat")));
                }
                
                if (entry.containsKey("memcardFilters")) {
                    List<String> memcardFilters = (List<String>) entry.get("memcardFilters");
                    
                    eb.addField(EmbedUtil.prebuildField(
                        "Memcard Filters", 
                        StringUtils.joinWith("\n", memcardFilters.toArray()), 
                        true));
                }
                
                if (entry.containsKey("roundModes")) {
                    Map<String, Object> roundModes = (Map<String, Object>) entry.get("roundModes");
                    
                    if (roundModes.containsKey("eeRoundMode")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "EE Rounding Mode", 
                            ROUNDING.get((int) roundModes.get("eeRoundMode")), 
                            true));
                    }

                    if (roundModes.containsKey("vuRoundMode")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "VU0 + VU1 Rounding Mode", 
                            ROUNDING.get((int) roundModes.get("vuRoundMode")), 
                            true));
                    }
                    
                    if (roundModes.containsKey("vu0RoundMode")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "VU1 Rounding Mode", 
                            ROUNDING.get((int) roundModes.get("vu0RoundMode")), 
                            true));
                    }

                    if (roundModes.containsKey("vu1RoundMode")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "VU1 Rounding Mode", 
                            ROUNDING.get((int) roundModes.get("vu1RoundMode")), 
                            true));
                    }
                }
                
                if (entry.containsKey("speedHacks")) {
                    Map<String, Object> speedhacks = (Map<String, Object>) entry.get("speedHacks");

                    if (speedhacks.containsKey("mvuFlag")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "MicroVU Flag Hack", 
                            GENERIC_BOOLEAN.get((int) speedhacks.get("mvuFlag")), 
                            true));
                    }
                    
                    if (speedhacks.containsKey("instantVU1")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Instant VU1", 
                            GENERIC_BOOLEAN.get((int) speedhacks.get("instantVU1")), 
                            true));
                    }
                    
                    if (speedhacks.containsKey("mtvu")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Multi-Threaded VU1 (MTVU)", 
                            GENERIC_BOOLEAN.get((int) speedhacks.get("mtvu")), 
                            true));
                    }

                    if (speedhacks.containsKey("eeCycleRate")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "EE Cycle Rate", 
                            EE_CYCLE_RATE.get((int) speedhacks.get("eeCycleRate")), 
                            true));
                    }
                }
                
                if (entry.containsKey("clampModes")) {
                    Map<String, Object> clampModes = (Map<String, Object>) entry.get("clampModes");
                    
                    if (clampModes.containsKey("eeClampMode")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "EE Clamping Mode", 
                            EE_CLAMPING.get((int) clampModes.get("eeClampMode")), 
                            true));
                    }
                    
                    if (clampModes.containsKey("vuClampMode")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "VU0 + VU1 Clamping Mode", 
                            VU_CLAMPING.get((int) clampModes.get("vuClampMode")), 
                            true));
                    }

                    if (clampModes.containsKey("vu0ClampMode")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "VU0 Clamping Mode", 
                            VU_CLAMPING.get((int) clampModes.get("vu0ClampMode")), 
                            true));
                    }
                    
                    if (clampModes.containsKey("vu1ClampMode")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "VU1 Clamping Mode", 
                            VU_CLAMPING.get((int) clampModes.get("vu1ClampMode")), 
                            true));
                    }
                }
                
                if (entry.containsKey("gameFixes")) {
                    List<String> gameFixes = (List<String>) entry.get("gameFixes");
                    
                    eb.addField(EmbedUtil.prebuildField(
                        "Game Fixes", 
                        StringUtils.joinWith("\n", gameFixes.toArray()), 
                        true));
                }
                
                if (entry.containsKey("gsHWFixes")) {
                    Map<String, Object> gsHWFixes = (Map<String, Object>) entry.get("gsHWFixes");
                    
                    if (gsHWFixes.containsKey("autoFlush")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Auto Flush", 
                            AUTO_FLUSH.get((int) gsHWFixes.get("autoFlush")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("conservativeFramebuffer")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Conservative Framebuffer", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("conservativeFramebuffer")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("cpuFramebufferConversion")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Frame Buffer Conversion", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("cpuFramebufferConversion")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("disableDepthSupport")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Disable Depth Emulation", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("disableDepthSupport")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("wrapGSMem")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Memory Wrapping", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("wrapGSMem")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("preloadFrameData")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Preload Frame Data", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("preloadFrameData")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("disablePartialInvalidation")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Disable Partial Invalidation", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("disablePartialInvalidation")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("textureInsideRT")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Texture Inside RT", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("textureInsideRT")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("alignSprite")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Align Sprite", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("alignSprite")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("mergeSprite")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Merge Sprite", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("mergeSprite")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("wildArmsHack")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Wild Arms Hack", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("wildArmsHack")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("pointListPalette")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Disable Safe Features", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("pointListPalette")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("mipmap")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Mipmapping", 
                            MIPMAP.get((int) gsHWFixes.get("mipmap")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("trilinearFiltering")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Trilinear Filtering", 
                            TRILINEAR.get((int) gsHWFixes.get("trilinearFiltering")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("skipDrawStart")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Skipdraw Range (Start)", 
                            String.valueOf((int) gsHWFixes.get("skipDrawStart")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("skipDrawEnd")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Skipdraw Range (End)",
                            String.valueOf((int) gsHWFixes.get("skipDrawEnd")),
                            true));
                    }

                    if (gsHWFixes.containsKey("getSkipCount")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Get Skip Count",
                            (String) gsHWFixes.get("getSkipCount"),
                            true));
                    }

                    if (gsHWFixes.containsKey("beforeDraw")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Before Draw",
                            (String) gsHWFixes.get("beforeDraw"),
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("halfBottomOverride")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Half Screen Fix", 
                            HALF_BOTTOM.get((int) gsHWFixes.get("halfBottomOverride")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("halfPixelOffset")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Half Pixel Offset", 
                            HALF_PIXEL_OFFSET.get((int) gsHWFixes.get("halfPixelOffset")), true));
                    }
                    
                    if (gsHWFixes.containsKey("roundSprite")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Round Sprite", 
                            ROUND_SPRITE.get((int) gsHWFixes.get("roundSprite")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("texturePreloading")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Texture Preloading", 
                            TEXTURE_PRELOADING.get((int) gsHWFixes.get("texturePreloading")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("deinterlace")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Deinterlacing",
                            DEINTERLACING.get((int) gsHWFixes.get("deinterlace")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("cpuSpriteRenderBW")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "CPU Sprite Render Size", 
                            String.valueOf((int) gsHWFixes.get("cpuSpriteRenderBW")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("cpuCLUTRender")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Software CLUT Render", 
                            String.valueOf((int) gsHWFixes.get("cpuCLUTRender")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("gpuTargetCLUT")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "GPU Target CLUT", 
                            GPU_TARGET_CLUT.get((int) gsHWFixes.get("gpuTargetCLUT")),
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("gpuPaletteConversion")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "GPU Palette Conversion",
                            GPU_PALETTE_CONVERSION.get((int) gsHWFixes.get("gpuPaletteConversion")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("recommendedBlendingLevel")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Blending Accuracy (Recommended)",
                            BLENDING_ACCURACY.get((int) gsHWFixes.get("recommendedBlendingLevel")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("maximumBlendingLevel")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Blending Accuracy (Maximum)",
                            BLENDING_ACCURACY.get((int) gsHWFixes.get("maximumBlendingLevel")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("minimumBlendingLevel")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Blending Accuracy (Minimum)",
                            BLENDING_ACCURACY.get((int) gsHWFixes.get("minimumBlendingLevel")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("PCRTCOffsets")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Screen Offsets", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("PCRTCOffsets")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("PCRTCOverscan")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Show Overscan", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("PCRTCOverscan")), 
                            true));
                    }
                    
                    if (gsHWFixes.containsKey("cpuSpriteRenderLevel")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "CPU Sprite Render Level", 
                            CPU_SPRITE_RENDER_LEVEL.get((int) gsHWFixes.get("cpuSpriteRenderLevel")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("nativePaletteDraw")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Unscaled Palette Texture Draws", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("nativePaletteDraw")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("bilinearUpscale")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Bilinear Dirty Upscale", 
                            GENERIC_BOOLEAN.get((int) gsHWFixes.get("bilinearUpscale")), 
                            true));
                    }

                    if (gsHWFixes.containsKey("nativeScaling")) {
                        eb.addField(EmbedUtil.prebuildField(
                            "Native Scaling", 
                            NATIVE_SCALING.get((int) gsHWFixes.get("nativeScaling")), 
                            true));
                    }
                }
                
                if (entry.containsKey("patches")) {
                    Object patchesObj = entry.get("patches");

                    try {
                        Map<String, Object> patches = (Map<String, Object>) patchesObj;    

                        for (String crcName : patches.keySet()) {
                            Map<String, Object> patch = (Map<String, Object>) patches.get(crcName);
                            
                            if (patch.containsKey("content")) {
                                String content = (String) patch.get("content");
                                eb.addField(EmbedUtil.prebuildField(
                                    "Patch (" + crcName + ")", 
                                    content, 
                                    false));
                            }
                        }
                    } catch (Exception e) {
                        Map<Integer, Object> patches = (Map<Integer, Object>) patchesObj;    

                        for (Integer crcName : patches.keySet()) {
                            Map<String, Object> patch = (Map<String, Object>) patches.get(crcName);
                            
                            if (patch.containsKey("content")) {
                                String content = (String) patch.get("content");
                                eb.addField(EmbedUtil.prebuildField(
                                    "Patch (" + crcName + ")", 
                                    content, 
                                    false));
                            }
                        }
                    }
                }
            }
        }
        
        if (!entryFound) {
            eb.setTitle("No GameIndex Entry Found");
            eb.setDescription("Serial `" + serial + "` did not appear anywhere in GameIndex.yaml - Verify spelling and try again.");
        }
        
        return eb.build();
    }
}
