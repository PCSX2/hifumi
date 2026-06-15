package net.pcsx2.hifumi.charting;

public class SpamkickChartData implements Comparable<SpamkickChartData> {
    public String timeUnit;
    public int events;
    public String trigger;
    
    @Override
    public int compareTo(SpamkickChartData otherData) {
        String[] thisParts = this.timeUnit.split("-");
        int thisYear = Integer.valueOf(thisParts[0]), thisMonth = Integer.valueOf(thisParts[1]), thisDay = Integer.valueOf(thisParts[2]);
        String[] otherParts = otherData.timeUnit.split("-");
        int otherYear = Integer.valueOf(otherParts[0]), otherMonth = Integer.valueOf(otherParts[1]), otherDay = Integer.valueOf(otherParts[2]);
        
        int res;
        
        if ((res = Integer.compare(thisYear, otherYear)) != 0) {
            return res;
        }
        
        if ((res = Integer.compare(thisMonth, otherMonth)) != 0) {
            return res;
        }
        
        return Integer.compare(thisDay, otherDay);
    }
}
