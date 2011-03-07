/*
 *    Copyright 2009-2010 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package savant.settings;

import javax.swing.*;

/**
 *
 * @author mfiume
 */
public class BrowserSettings {

    private static PersistentSettings settings = PersistentSettings.getInstance();

    private static final String CHECKVERSION_KEY = "CHECKVERSION";
    private static final String COLLECTSTATS_KEY = "COLLECTSTATS";
    private static final String CACHINGENABLED_KEY = "CACHINGENABLED";
    
    /*
     * Remote Files
     */
    private static final String REMOTE_BUFFER_SIZE = "REMOTE_BUFFER_SIZE";
    public static final int DEFAULT_BUFFER_SIZE = 65536;

    /*
     * Website
     */
    public static final String url = "http://www.savantbrowser.com";
    public static String url_version = url + "/serve/version/version.xml";
    public static String url_data = url + "/serve/data/data.xml";
    public static String url_plugin = url + "/serve/plugin/plugin.xml";
    public static String url_logusagestats = url + "/scripts/logUsageStats.cgi";
    public static String url_tutorials = url + "/media.html";
    public static String url_manuals = url + "/documentation.html";
    public static String safe = url + "/safe/savantsafe.php";


    public static String version = "1.4.4";
    public static String build = "developer";

    public static boolean getCheckVersionOnStartup() {
        return settings.getBoolean(CHECKVERSION_KEY, true);
    }

    public static boolean getCollectAnonymousUsage() {
        return settings.getBoolean(COLLECTSTATS_KEY, true);
    }

    public static boolean getCachingEnabled() {
        return settings.getBoolean(CACHINGENABLED_KEY, true);
    }

    public static int getRemoteBufferSize(){
        String s = settings.getString(REMOTE_BUFFER_SIZE);
        return s != null ? Integer.parseInt(s) : DEFAULT_BUFFER_SIZE;
    }

    public static void setCheckVersionOnStartup(boolean b) {
        settings.setBoolean(CHECKVERSION_KEY, b);
    }

    public static void setCollectAnonymousUsage(boolean b) {
        settings.setBoolean(COLLECTSTATS_KEY, b);
    }

    public static void setCachingEnabled(boolean b) {
        settings.setBoolean(CACHINGENABLED_KEY, b);
    }

    public static void setRemoteBufferSize(int size){
        settings.setString(REMOTE_BUFFER_SIZE, String.valueOf(size));
    }

    /**
     * Look and Feel
     */
    public static String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
    //public static Class LookAndFeelAddon = MetalLookAndFeelAddons.class;

    /**
     * padding
     */
    public static int padding = 7;

    /**
     * Zooming
     */
    public static int zoomAmount = 2;

    /**
     * Fonts
     */
    public static String fontName = "Verdana";

}
