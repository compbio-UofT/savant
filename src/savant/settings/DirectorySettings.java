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

/**
 *
 * @author AndrewBrook
 */
public class DirectorySettings {


    private static String SAVANT_DIR;
    private static String formatDir;
    private static String pluginsDir;
    private static String XMLToolsDir;

    public static String getSavantDirectory() {
        if (SAVANT_DIR == null) {
            String os = System.getProperty("os.name").toLowerCase();
            String home = System.getProperty("user.home");
            String fileSeparator = System.getProperty("file.separator");
            if (os.contains("win")) {
                SAVANT_DIR = home + fileSeparator + "savant";
            }
            else {
                SAVANT_DIR = home + fileSeparator + ".savant";
            }
            File dir = new File(SAVANT_DIR);
            if (!dir.exists()) {
                dir.mkdir();
            }
        }
        return SAVANT_DIR;
    }

    public static String getFormatDirectory() {
        if(formatDir == null){
            //String home = System.getProperty("user.home");
            String home = getSavantDirectory();
            String fileSeparator = System.getProperty("file.separator");
            formatDir = home + fileSeparator + "savant_files";
            File dir = new File(formatDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return formatDir;
    }

    public static void setFormatDirectory(String dir) {
        File f = new File(dir);
        if (!f.exists()) {
            boolean ret = f.mkdirs();
            if(ret) formatDir = dir;
        }      
    }

    public static String getXMLToolDescriptionsDirectory() {
        if(XMLToolsDir == null){
            //String home = System.getProperty("user.home");
            String home = getSavantDirectory();
            String fileSeparator = System.getProperty("file.separator");
            XMLToolsDir = home + fileSeparator + "savant_xmltools";
            File dir = new File(XMLToolsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return XMLToolsDir;
    }

    public static void setXMLToolDescriptionsDirectory(String dir) {
        File f = new File(dir);
        if (!f.exists()) {
            boolean ret = f.mkdirs();
            if(ret) XMLToolsDir = dir;
        }
    }

    public static String getPluginsDirectory() {
        if(pluginsDir == null){
            //String home = System.getProperty("user.home");
            String home = getSavantDirectory();
            String fileSeparator = System.getProperty("file.separator");
            pluginsDir = home + fileSeparator + "savant_plugins";
            File dir = new File(pluginsDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        return pluginsDir;
    }

    public static void setPluginsDirectory(String dir) {
        File f = new File(dir);
        if (!f.exists()) {
            boolean ret = f.mkdirs();
            if(ret) pluginsDir = dir;
        }
    }

}
