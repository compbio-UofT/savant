/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.controller;

import java.io.File;
import savant.settings.DirectorySettings;

/**
 *
 * @author mfiume
 */
public class FileController {

    public static String createPluginsDirectory(String pluginName, String sessionId) {
        String pluginDir = DirectorySettings.getPluginsDirectory();
        String fileSeparator = System.getProperty("file.separator");

        String newDirName = pluginDir + fileSeparator + pluginName + fileSeparator + sessionId;
        File dir = new File(newDirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return newDirName;
    }
}
