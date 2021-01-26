package com.wesecureapp.plugins.strobes;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import groovy.json.JsonSlurper;
import groovy.json.internal.LazyMap;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ScanHelper {
    private String baseUrl;
    private String apiKey;
    private String target;
    private int configId;
    private String connectorType;
    private Logger logger = Logger.getLogger(ScanHelper.class.getName());

    public ScanHelper(String baseUrlParam, String apikeyParam, String targetParam, int configIdParam,
            String connectorTypeParam) {
        this.baseUrl = baseUrlParam;
        this.apiKey = apikeyParam;
        this.target = targetParam;
        this.configId = configIdParam;
        this.connectorType = connectorTypeParam;
    }

    public ScanResult startScan() {
        try {
            String apiUrl = String.format("%s/api/v1/cicd/scan/", baseUrl);
            URL endpoint = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
            conn.setRequestMethod("POST");
            // set headers
            conn.setRequestProperty("Authorization", apiKey);
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // write data Object
            JSONObject body = new JSONObject();
            body.put("configuration_id", configId);
            if (this.connectorType.equals(ConnectorType.DAST)) {
                JSONArray urls = new JSONArray();
                urls.add(Arrays.stream(target.split(",")).map(String::trim).toArray(String[]::new));
                body.put("target_list", urls);
            } else {
                body.put("target", target);
            }

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = body.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200 && responseCode != 201) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }

                    JSONObject respObj = JSONObject.fromObject(response.toString());

                    if (null == respObj) {
                        throw new ValidationException("Invalid result");
                    } else {
                        return extractScanResult(respObj);
                    }
                }
            }

        } catch (RuntimeException re) {
            logger.log(Level.ALL, re.getMessage());
            return null;
        } catch (Exception e) {
            logger.log(Level.ALL, e.getMessage());
            return null;
        }
    }

    public ScanResult getScanStatusbyId(String taskId) throws ValidationException {
        try {
            String apiUrl = String.format("%s/api/v1/cicd/status/%s/", baseUrl, taskId);
            URL endpoint = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
            conn.setRequestMethod("GET");
            // set headers
            conn.setRequestProperty("Authorization", apiKey);
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            } else {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    JSONObject respObj = JSONObject.fromObject(response.toString());

                    if (null == respObj) {
                        throw new ValidationException("Invalid result");
                    } else {

                        return extractScanResult(respObj);
                    }
                }
            }

        } catch (Exception e) {
            throw new ValidationException(e);
        }
    }

    private ScanResult extractScanResult(JSONObject respObj) {
        ScanResult result = new ScanResult();
        String empty = "";
        result.setBaseUrl(baseUrl);
        result.setTaskId(respObj.getOrDefault("task_id", empty).toString());
        result.setStatus(Integer.parseInt(respObj.getOrDefault("status", empty).toString()));
        result.setBuildStatus(Boolean.parseBoolean(respObj.getOrDefault("build_status", "false").toString()));
        result.setOrganizationId(respObj.getOrDefault("organization_id", empty).toString());

        if (respObj.has("bug_stats")) {

            JSONObject bugStatsObj = respObj.getJSONObject("bug_stats");
            if (null != bugStatsObj && bugStatsObj.size() > 0) {
                BugStats bugStats = new BugStats();
                bugStats.fromJSONObject(bugStatsObj);
                result.setBugStats(bugStats);
            }
        }
        return result;
    }
}
