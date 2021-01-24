package com.wesecureapp.plugins.strobes;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Example of Jenkins global configuration.
 */
@Extension
public class ScanGlobalConfiguration extends GlobalConfiguration {

    /** @return the singleton instance */
    public static ScanGlobalConfiguration get() {
        return ExtensionList.lookupSingleton(ScanGlobalConfiguration.class);
    }

    private String baseUrl;
    private String apiKey;

    public ScanGlobalConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    /** @return the currently configured label, if any */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Together with {@link #getLabel}, binds to entry in {@code config.jelly}.
     * 
     * @param label the new value of this field
     */
    @DataBoundSetter
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        save();
    }

    public String getApiKey() {
        return apiKey;
    }

    @DataBoundSetter
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
        save();
    }

    public FormValidation doCheckBaseUrl(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.error(Messages.ScanConfiguration_errors_invalidUrl());
        }

        if (!Utility.isValidUrl(value)) {
            return FormValidation.error(Messages.ScanConfiguration_errors_invalidUrl());
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckApiKey(@QueryParameter String value) {
        if (StringUtils.isEmpty(value)) {
            return FormValidation.error(Messages.ScanConfiguration_errors_missingApiKey());
        }
        return FormValidation.ok();
    }

}
