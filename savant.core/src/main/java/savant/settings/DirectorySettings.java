/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

    private static File savantDir;

    private static final String CACHE_DIR_KEY = "CacheDir";
    private static final String PLUGINS_DIR_KEY = "PluginsDir";
    private static final String PROJECTS_DIR_KEY = "ProjectsDir";

    public static File getSavantDirectory() {
        if (savantDir == null) {
            File f = new File(System.getProperty("user.home"), MiscUtils.WINDOWS ? "savant" : ".savant");
            if (!f.exists()) {
                f.mkdir();
            }
            savantDir = f;
        }
        return savantDir;
    }

    public static File getLibsDirectory() {
        if (MiscUtils.MAC) {
            File result = new File(com.apple.eio.FileManager.getPathToApplicationBundle() + "/Contents/Resources/Java");
            if (result.exists()) {
                return result;
            }
        }
        return new File("lib");
    }

    private static File getDirectory(String key, String dirName) {
        File result = settings.getFile(key);
        if (result == null) {
            result = new File(getSavantDirectory(), dirName);
        }
        if (!result.exists()) {
            result.mkdirs();
        }
        return result;
    }

    private static void setDirectory(String key, File value) {
        if (!value.exists()) {
            value.mkdirs();
        }
        settings.setFile(key, value);
    }

    public static File getCacheDirectory() {
        return getDirectory(CACHE_DIR_KEY, "cache");
    }

    public static File getPluginsDirectory(){
        return getDirectory(PLUGINS_DIR_KEY, "plugins");
    }

    public static File getProjectsDirectory() {
        return getDirectory(PROJECTS_DIR_KEY, "projects");
    }

    public static File getTmpDirectory() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public static void setPluginsDirectory(File dir) {
        setDirectory(PLUGINS_DIR_KEY, dir);
    }

    public static void setCacheDirectory(File dir) {
        setDirectory(CACHE_DIR_KEY, dir);
    }
}
