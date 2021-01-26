package com.wesecureapp.plugins.strobes;

import net.sf.json.JSONObject;

public class ScanResult {
    private String taskId;
    // Will have 3 states:
    // Status = 1, Started
    // Status = 2, Failed at strobes side
    // Status = 3, Completed
    private int status;

    // When Status = 3, False implies there are bugs
    private boolean buildStatus;
    private BugStats bugStats;
    private String baseUrl;
    private String organizationId;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isBuildStatus() {
        return buildStatus;
    }

    public void setBuildStatus(boolean buildStatus) {
        this.buildStatus = buildStatus;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public BugStats getBugStats() {
        return bugStats;
    }

    public void setBugStats(BugStats bugStats) {
        this.bugStats = bugStats;
    }

    private String getBugsLink() {

        return String.format(
                "%s/organizations/%s/bugs/?severity[]=1&severity[]=2&severity[]=3&severity[]=4&severity[]=5&state[]=1&state[]=2&state[]=3&state[]=4&taskId=%s",
                this.getBaseUrl(), this.getOrganizationId(), this.getTaskId());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("---------- Scan Result Summary ---------- %n"));
        sb.append(String.format("Task: %s %n", this.getTaskId()));
        sb.append(String.format("Scan status: %s %n", this.getStatus()));
        sb.append(String.format("Build status: %s %n", this.isBuildStatus()));
        if (null != bugStats) {
            sb.append(String.format("Bug Stats: %n"));
            sb.append(this.bugStats.toString());
            sb.append(String.format("Bug Link: %s %n", this.getBugsLink()));
        }
        sb.append(String.format("-------------------- %n"));
        return sb.toString();
    }
}
