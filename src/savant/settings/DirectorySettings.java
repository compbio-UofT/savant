/*
 *    Copyright 2010 University of Toronto
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

import java.io.File;
import savant.util.MiscUtils;


/**
 *
 * @author AndrewBrook
 */
public class DirectorySettings {
    private static PersistentSettings settings = PersistentSettings.getInstance();

    private static String savantDir;

    private static final String CACHE_DIR_KEY = "CacheDir";
    private static final String FORMAT_DIR_KEY = "FormatDir";
    private static final String TMP_DIR_KEY = "TmpDir";
    private static final String PLUGINS_DIR_KEY = "PluginsDir";
    private static final String XML_TOOLS_DIR_KEY = "XMLToolDir";
    private static final String PROJECTS_DIR_KEY = "ProjectsDir";

    public static String getSavantDirectory() {
        if (savantDir == null) {
            String os = System.getProperty("os.name").toLowerCase();
            File f = new File(System.getProperty("user.home"), os.contains("win") ? "savant" : ".savant");
            if (!f.exists()) {
                f.mkdir();
            }
            savantDir = f.getAbsolutePath();
        }
        return savantDir;
    }

    public static String getLibsDirectory() {
        if (MiscUtils.MAC) {
            String bundlePath = com.apple.eio.FileManager.getPathToApplicationBundle();
            return bundlePath + "/Contents/Resources/Java";
        }
        return "lib";
    }

    private static String getDirectory(String key, String dirName) {
        File result = settings.getFile(key);
        if (result == null) {
            result = new File(getSavantDirectory(), dirName);
        }
        if (!result.exists()) {
            result.mkdirs();
        }
        return result.getAbsolutePath();
    }

    private static void setDirectory(String key, String value) {
        File dir = new File(value);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        settings.setFile(key, dir);
    }

    public static String getCacheDirectory() {
        return getDirectory(CACHE_DIR_KEY, "cache");
    }

    public static String getFormatDirectory() {
        return getDirectory(FORMAT_DIR_KEY, "formatted_files");
    }

    /**
     * Retrieve the preference for the plugins directory.  Unlike the other directories,
     * its default location is <b>not</b> inside the <i>.savant</i> directory.
     * @return
     */
    /*public static String getPluginsDirectory() {
        File result = settings.getFile(PLUGINS_DIR_KEY);
        if (result == null) {
            // First, determine whether we're a Mac application bundle by looking
            // for the magical Mac directory.  If it's not found, we'll just use ./plugins.
            result = new File("Savant.app/Contents/Plugins");
            if (!result.exists()) {
                result = new File("plugins");
            }
        }
        if (!result.exists()) {
            result.mkdirs();
        }
        return result.getAbsolutePath();
    }*/

    public static String getPluginsDirectory(){
        return getDirectory(PLUGINS_DIR_KEY, "plugins");
    }

    public static String getProjectsDirectory() {
        return getDirectory(PROJECTS_DIR_KEY, "projects");
    }

    public static String getTmpDirectory() {
        return getDirectory(TMP_DIR_KEY, "tmp");
    }

    public static String getXMLToolDescriptionsDirectory() {
        return getDirectory(XML_TOOLS_DIR_KEY, "xmltools");
    }

    public static void setFormatDirectory(String dir) {
        setDirectory(FORMAT_DIR_KEY, dir);
    }

    public static void setPluginsDirectory(String dir) {
        setDirectory(PLUGINS_DIR_KEY, dir);
    }

    public static void setXMLToolDescriptionsDirectory(String dir) {
        setDirectory(XML_TOOLS_DIR_KEY, dir);
    }

    public static void setCacheDirectory(String dir) {
        setDirectory(CACHE_DIR_KEY, dir);
    }
}
