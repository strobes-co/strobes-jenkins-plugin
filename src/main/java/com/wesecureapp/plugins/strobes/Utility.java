package com.wesecureapp.plugins.strobes;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utility {
    static boolean isValidUrl(String url) {
        try {
            URL obj = new URL(url);
            obj.toURI();
            return true;
        } catch (MalformedURLException e) {
            return false;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    static boolean isInvalidGitbranch(String value) {
        // Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Pattern special = Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~:]");
        Matcher match = special.matcher(value);
        return match.find();
    }

}
