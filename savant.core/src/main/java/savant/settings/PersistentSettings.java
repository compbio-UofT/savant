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

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.util.ColourKey;

/**
 * Class which makes settings persistent.
 *
 * @author tarkvara
 */
public class PersistentSettings extends Properties {
    protected static final Log LOG = LogFactory.getLog(PersistentSettings.class);

    private static final String SETTINGS_FILE = "savant.settings";
    private static PersistentSettings instance = null;

    public static PersistentSettings getInstance() {
        if (instance == null) {
            instance = new PersistentSettings();
            instance.load();
        }
        return instance;
    }

    public void load() {
        FileInputStream fis = null;
        try {
            File settingsFile = new File(DirectorySettings.getSavantDirectory(), SETTINGS_FILE);
            if (settingsFile.exists()) {
                fis = new FileInputStream(settingsFile);
                load(fis);
            }
        } catch (Exception x) {
            LOG.error("Unable to load savant.settings", x);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void store() throws IOException {
        super.store(new FileOutputStream(new File(DirectorySettings.getSavantDirectory(), SETTINGS_FILE)), "Savant settings");
    }

    public Color getColour(String key, Color dflt) {
        String value = getProperty(key);
        if (value != null) {
            try {
                value = value.toUpperCase();
                int r, g, b;
                int a = 255;
                if (value.length() >= 6) {
                    r = Integer.parseInt(value.substring(0, 2), 16);
                    g = Integer.parseInt(value.substring(2, 4), 16);
                    b = Integer.parseInt(value.substring(4, 6), 16);
                    if (value.length() == 8) {
                        a = Integer.parseInt(value.substring(6, 8), 16);
                    }
                    return new Color(r, g, b, a);
                } else {
                    // Couldn't find a valid colour string.
                    LOG.warn(String.format("Couldn't parse \"%s\" as a colour.", value));
                }
            } catch (NumberFormatException nfx) {
                LOG.warn(String.format("Couldn't parse \"%s\" as a colour.", value), nfx);
            }
        }
        return dflt;
    }

    public Color getColor(ColourKey key, Color dflt) {
        return getColour(key.toString(), dflt);
    }

    public void setColour(String key, Color value) {
        setProperty(key, String.format("%02X%02X%02X%02X", value.getRed(), value.getGreen(), value.getBlue(), value.getAlpha()));
    }

    public void setColor(ColourKey key, Color value) {
        setColour(key.toString(), value);
    }

    public boolean getBoolean(String key, boolean dflt) {
        String value = getProperty(key);
        return value != null ? value.toLowerCase().equals("true") : dflt;
    }

    public void setBoolean(String key, boolean value) {
        setProperty(key, Boolean.toString(value));
    }

    public File getFile(String key) {
        String value = getProperty(key);
        return value != null ? new File(value) : null;
    }

    public void setFile(String key, File value) {
        setProperty(key, value.getAbsolutePath());
    }

    public int getInt(String key, int dflt) {
        String value = getProperty(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
        }
        return dflt;
    }

    public void setInt(String key, int value) {
        setProperty(key, Integer.toString(value));
    }

    public String getString(String key){
        String value = getProperty(key);
        return value;
    }

    public void setString(String key, String value){
        if (value == null) {
            remove(key);
        } else {
            setProperty(key, value);
        }
    }
}
