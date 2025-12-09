package net.pcsx2.hifumi.charting;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

import net.pcsx2.hifumi.database.Database;
import net.pcsx2.hifumi.util.Messaging;

public class ChartGenerator {

    public static byte[] buildWarezChart(long startTimestamp, long endTimestamp, String timeUnit) {
        ArrayList<WarezChartData> warezDataList = Database.getWarezAssignmentsBetween(startTimestamp, endTimestamp, timeUnit);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (WarezChartData data : warezDataList) {
            dataset.addValue(data.events, data.action, data.timeUnit);
        }

        JFreeChart chart = ChartFactory.createBarChart("Warez Events (by " + timeUnit + ")", timeUnit, "Warez Events", dataset, PlotOrientation.HORIZONTAL, true, true, false);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(true);
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesPaint(1, Color.GREEN);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(out, chart, 1280, 720);
            return out.toByteArray();
        } catch (Exception e) {
            Messaging.logException("ChartGenerator", "buildWarezChart", e);
        }
        
        return null;
    }

    public static byte[] buildMemberChart(long startTimestamp, long endTimestamp, String timeUnit) {
        ArrayList<MemberChartData> memberDataList = Database.getMemberEventsBetween(startTimestamp, endTimestamp, timeUnit);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (MemberChartData data : memberDataList) {
            dataset.addValue(data.events, data.action, data.timeUnit);
        }

        JFreeChart chart = ChartFactory.createBarChart("Member Events (by " + timeUnit + ")", timeUnit, "Member Events", dataset, PlotOrientation.HORIZONTAL, true, true, false);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(true);
        renderer.setSeriesPaint(0, Color.GREEN);
        renderer.setSeriesPaint(1, Color.YELLOW);
        renderer.setSeriesPaint(2, Color.RED);
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(out, chart, 1280, 720);
            return out.toByteArray();
        } catch (Exception e) {
            Messaging.logException("ChartGenerator", "buildMemberChart", e);
        }
        
        return null;
    }

    public static byte[] buildAutomodChart(long startTimestamp, long endTimestamp, String timeUnit) {
        ArrayList<AutomodChartData> automodDataList = Database.getAutomodEventsBetween(startTimestamp, endTimestamp, timeUnit);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        
        for (AutomodChartData data : automodDataList) {
            dataset.addValue(data.events, data.trigger, data.timeUnit);
        }

        JFreeChart chart = ChartFactory.createBarChart("Automod Events (by " + timeUnit + ")", timeUnit, "Automod Events", dataset, PlotOrientation.HORIZONTAL, true, true, false);
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setDrawBarOutline(true);
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ChartUtils.writeChartAsPNG(out, chart, 1280, 720);
            return out.toByteArray();
        } catch (Exception e) {
            Messaging.logException("ChartGenerator", "buildAutomodChart", e);
        }
        
        return null;
    }
}
