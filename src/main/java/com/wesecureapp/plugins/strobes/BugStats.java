package com.wesecureapp.plugins.strobes;

import java.lang.ProcessHandle.Info;

import jenkins.mvn.GlobalSettingsProvider;
import net.sf.json.JSONObject;

public class BugStats {
    private int low;
    private int high;
    private int info;
    private int medium;
    private int critical;
    private int newBugs;
    private int totalBugs;
    private int duplicateBugs;
    private int totalBugsReported;
    private int resolvedBugs;

    public int getLow() {
        return low;
    }

    public int getNewBugs() {
        return newBugs;
    }

    public void setNewBugs(int newBugs) {
        this.newBugs = newBugs;
    }

    public int getTotalBugs() {
        return totalBugs;
    }

    public void setTotalBugs(int totalBugs) {
        this.totalBugs = totalBugs;
    }

    public int getDuplicateBugs() {
        return duplicateBugs;
    }

    public void setDuplicateBugs(int duplicateBugs) {
        this.duplicateBugs = duplicateBugs;
    }

    public int getTotalBugsReported() {
        return totalBugsReported;
    }

    public void setTotalBugsReported(int totalBugsReported) {
        this.totalBugsReported = totalBugsReported;
    }

    public int getResolvedBugs() {
        return resolvedBugs;
    }

    public void setResolvedBugs(int resolvedBugs) {
        this.resolvedBugs = resolvedBugs;
    }

    public int getCritical() {
        return critical;
    }

    public void setCritical(int critical) {
        this.critical = critical;
    }

    public int getMedium() {
        return medium;
    }

    public void setMedium(int medium) {
        this.medium = medium;
    }

    public void setLow(int low) {
        this.low = low;
    }

    public void setHigh(int high) {
        this.high = high;
    }

    public int getHigh() {
        return high;
    }

    public void setInfo(int info) {
        this.info = info;
    }

    public int getInfo() {
        return info;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("   Low: %s %n", this.getLow()));
        sb.append(String.format("   High: %s %n", this.getHigh()));
        sb.append(String.format("   Info: %s %n", this.getInfo()));
        sb.append(String.format("   Medium: %s %n", this.getMedium()));
        sb.append(String.format("   Critical: %s %n", this.getCritical()));
        sb.append(String.format("   New Bugs: %s %n", this.getNewBugs()));
        sb.append(String.format("   Total Bugs: %s %n", this.getTotalBugs()));
        sb.append(String.format("   Duplicate Bugs: %s %n", this.getDuplicateBugs()));
        sb.append(String.format("   Total Bugs Reported: %s %n", this.getTotalBugsReported()));
        sb.append(String.format("   Already Resolved Bugs: %s %n", this.getResolvedBugs()));
        return sb.toString();
    }

    public void fromJSONObject(JSONObject bugStatsObj) {
        if (null != bugStatsObj) {
            this.setLow(Integer.parseInt(bugStatsObj.get("low").toString()));
            this.setHigh(Integer.parseInt(bugStatsObj.get("high").toString()));
            this.setInfo(Integer.parseInt(bugStatsObj.get("info").toString()));
            this.setMedium(Integer.parseInt(bugStatsObj.get("medium").toString()));
            this.setCritical(Integer.parseInt(bugStatsObj.get("critical").toString()));
            this.setNewBugs(Integer.parseInt(bugStatsObj.get("new_bugs").toString()));
            this.setTotalBugs(Integer.parseInt(bugStatsObj.get("total_bugs").toString()));
            this.setDuplicateBugs(Integer.parseInt(bugStatsObj.get("duplicate_bugs").toString()));
            this.setTotalBugsReported(Integer.parseInt(bugStatsObj.get("total_bugs_reported").toString()));
            this.setResolvedBugs(Integer.parseInt(bugStatsObj.get("already_resolved_bugs").toString()));
        }
    }

}
