package com.wesecureapp.plugins.strobes;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import groovy.json.JsonSlurper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.GlobalConfiguration;
import jenkins.tasks.SimpleBuildStep;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;
import com.wesecureapp.plugins.strobes.ScanHelper;

public class ScanBuilder extends Builder implements SimpleBuildStep {

    private final String name;
    // will hold stop, continue
    private boolean buildCriteria;
    private int waitTimeInSec = 10;
    private String scanType;
    private String target;
    private final static Logger logger = Logger.getLogger(ScanBuilder.class.getName());

    @DataBoundConstructor
    public ScanBuilder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setBuildCriteria(boolean buildCriteria) {
        this.buildCriteria = buildCriteria;
    }

    public boolean getBuildCriteria() {
        return buildCriteria;
    }

    @DataBoundSetter
    public void setWaitTimeInSec(int waitTimeInSec) {
        this.waitTimeInSec = waitTimeInSec;
    }

    public int getWaitTimeInSec() {
        return waitTimeInSec;
    }

    @DataBoundSetter
    public void setScanType(String scanType) {
        this.scanType = scanType;
    }

    public String getScanType() {
        return scanType;
    }

    @DataBoundSetter
    public void setTarget(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }

    private String configToString(ScanGlobalConfiguration config) {
        StringBuilder sb = new StringBuilder();
        sb.append("*****************************" + System.lineSeparator());
        sb.append(String.format("Strobes Scan Configuration %n "));
        // sb.append(String.format("Name: %s %n ", this.getName()));
        sb.append(String.format("Endpoint: %s %n ", config.getBaseUrl()));
        sb.append(String.format("Build Criteria: %s %n ", this.getBuildCriteria()));
        sb.append(String.format("Wait time in secs: %s %n ", this.getWaitTimeInSec()));
        sb.append(String.format("Scan Configuration: Config: %s, Connector : %s %n ",
                this.getConfigurationId(this.getScanType()), this.getConnectorType(this.getScanType())));
        sb.append(String.format("Target: %s %n ", this.getTarget()));
        sb.append("***************************** " + System.lineSeparator());

        return sb.toString();
    }

    private String getConfigurationId(String scanType) {
        return null != scanType && scanType.length() > 0 ? scanType.split(":")[0] : "";
    };

    private String getConnectorType(String scanType) {
        return null != scanType && scanType.length() > 0 ? scanType.split(":")[1] : "";
    };

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) {
        if (build == null)
            return false;
        ScanGlobalConfiguration scanGlobalConfig = GlobalConfiguration.all().get(ScanGlobalConfiguration.class);
        // Now we have necessary configuration
        if (null != scanGlobalConfig) {
            listener.getLogger().println(configToString(scanGlobalConfig));
            // So start the scan and follow up status
            String connectorTypeId = null != scanType && scanType.length() > 0 ? scanType.split(":")[1] : "";
            int configId = null != scanType && scanType.length() > 0 ? Integer.parseInt(scanType.split(":")[0]) : 0;
            ScanHelper scanHelper = new ScanHelper(scanGlobalConfig.getBaseUrl(), scanGlobalConfig.getApiKey(), target,
                    configId, connectorTypeId);
            try {
                ScanResult result = scanHelper.startScan();
                if (null == result && buildCriteria) {
                    listener.getLogger().append("Error in connecting to Strobes Scan API, Failing build as configured."
                            + System.lineSeparator());
                    return false;
                }
                // listener.getLogger().
                if (null != result) {
                    if (!result.isBuildStatus()) {
                        // wait for time and recheck
                        try {
                            TimeUnit.SECONDS.sleep(waitTimeInSec);
                            ScanResult rescanResult = scanHelper.getScanStatusbyId(result.getTaskId());
                            if (rescanResult.isBuildStatus()) {
                                return true;
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        return true;
                    }
                }
            } catch (ValidationException e) {
                listener.getLogger().append(e.getMessage() + System.lineSeparator());
                // based on build criteria execution should work
                return buildCriteria;
            }
        } else {
            listener.getLogger().append("Strobes Scan is not configured properly" + System.lineSeparator());
        }
        return true;
    }

    @Symbol("strobes")
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private final Logger logger = Logger.getLogger(DescriptorImpl.class.getName());

        public ListBoxModel doFillScanTypeItems(@QueryParameter String value) {
            ListBoxModel model = new ListBoxModel();
            ScanGlobalConfiguration scanGlobalConfiguration = GlobalConfiguration.all()
                    .get(ScanGlobalConfiguration.class);

            if (null == scanGlobalConfiguration) {
                // to-do: log the error
                this.logger.error("Invalid Global Strobes configuration");
                return model;
            }

            try {
                List<Map> fetchedConfigurations = getConfigurations(scanGlobalConfiguration.getBaseUrl(),
                        scanGlobalConfiguration.getApiKey());
                if (null != fetchedConfigurations) {
                    for (Map fetchedConfig : fetchedConfigurations) {
                        model.add(fetchedConfig.get("name").toString(), String.format("%s:%s",
                                fetchedConfig.get("id").toString(), fetchedConfig.get("connector_type").toString()));
                    }
                }
            } catch (Exception e) {
                this.logger.error(e);
                model = new ListBoxModel();
            }
            return model;
        }

        // will not throw execption, shoudl handle in callee
        private List<Map> getConfigurations(String baseUrl, String apiKey) throws Exception {

            String apiUrl = String.format("%s/api/v1/cicd/configurations/", baseUrl);
            URL endpoint = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
            conn.setRequestMethod("GET");
            // set headers
            conn.setRequestProperty("Authorization", apiKey);
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("user-agent",
                    "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0) Gecko/20100101 Firefox/10.0");
            // conn.setRequestProperty("accept", "*/*");
            // conn.setRequestProperty("accept-encoding", "gzip, deflate, br");
            // conn.setRequestProperty("connection", "keep-alive");
            // conn.setReadTimeout(10000);
            // conn.setConnectTimeout(5000);
            // conn.connect();
            conn.setDoInput(true);
            conn.setDoOutput(true);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            } else {
                // StringBuilder respBuilder = new StringBuilder();
                // Scanner scanner = new Scanner(endpoint.openStream(), "UTF-8");

                // // Write all the JSON data into a string using a scanner
                // while (scanner.hasNext()) {
                // respBuilder.append(scanner.nextLine());
                // }

                // // Close the scanner
                // scanner.close();

                // JsonSlurper parser = new JsonSlurper();
                // return (List<Map>) parser.parseText(respBuilder.toString());

                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JsonSlurper jSlurper = new JsonSlurper();
                    return (List<Map>) jSlurper.parseText(response.toString());
                }

            }
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            if (value.length() == 0)
                return FormValidation.error(Messages.StrobesScanBuilder_DescriptorImpl_errors_missingUrl());

            // Check for a valid Url
            if (!Utility.isValidUrl(value)) {
                return FormValidation.error(Messages.StrobesScanBuilder_DescriptorImpl_errors_invalidUrl());
            }

            return FormValidation.ok();
        }

        // Validation for Target
        public FormValidation doCheckTarget(@QueryParameter String value, @QueryParameter String scanType) {

            if (StringUtils.isBlank(value))
                return FormValidation.error(Messages.StrobesScanBuilder_DescriptorImpl_errors_missingTarget());

            // Target can be url or string
            // so check regex for branch-name, if fails check for url
            if (null != scanType && scanType.length() > 0) {
                String connectorType = scanType.split(":")[1];
                if (connectorType.equals("2")) {
                    // check for comma seperated Url
                    String[] dastUrls = value.split(",");
                    for (String dastUrl : dastUrls) {
                        if (!Utility.isValidUrl(dastUrl)) {
                            return FormValidation.error(String.format("%s : %s",
                                    Messages.StrobesScanBuilder_DescriptorImpl_errors_invalidUrl(), dastUrl));
                        }
                    }

                } else if (Utility.isInvalidGitbranch(value)) {
                    return FormValidation.error(Messages.StrobesScanBuilder_DescriptorImpl_errors_invalidTarget());
                }
            }

            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.StrobesScanBuilder_DescriptorImpl_DisplayName();
        }

    }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {
        // TODO Auto-generated method stub

    }

}
