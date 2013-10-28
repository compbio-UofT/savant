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
package savant.api.util;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.plugin.SavantPlugin;
import savant.settings.PersistentSettings;
import savant.util.CryptoUtils;


/**
 * Utility functions to allow plugins to store their settings in a central location.
 *
 * @author tarkvara
 */
public class SettingsUtils {
    private static final Log LOG = LogFactory.getLog(SettingsUtils.class);

    private static String makePluginKey(SavantPlugin p, String key) {
        return p.getDescriptor().getID() + "." + key;
    }

    /**
     * Get a colour setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param dflt a default value if the setting is not found
     * @return the setting's value (or dflt if not found)
     */
    public static Color getColour(SavantPlugin p, String key, Color dflt) {
        return PersistentSettings.getInstance().getColour(makePluginKey(p, key), dflt);
    }

    /**
     * Store a colour setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param value the value to be set
     */
    public static void setColour(SavantPlugin p, String key, Color value) {
        PersistentSettings.getInstance().setColour(makePluginKey(p, key), value);
    }

    /**
     * Get a boolean setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param dflt a default value if the setting is not found
     * @return the setting's value (or dflt if not found)
     */
    public static boolean getBoolean(SavantPlugin p, String key, boolean dflt) {
        return PersistentSettings.getInstance().getBoolean(makePluginKey(p, key), dflt);
    }

    /**
     * Store a boolean setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param value the value to be set
     */
    public static void setBoolean(SavantPlugin p, String key, boolean value) {
        PersistentSettings.getInstance().setBoolean(makePluginKey(p, key), value);
    }

    /**
     * Get an integer setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param dflt a default value if the setting is not found
     * @return the setting's value (or dflt if not found)
     * @since 1.6.0
     */
    public static int getInt(SavantPlugin p, String key, int dflt) {
        return PersistentSettings.getInstance().getInt(makePluginKey(p, key), dflt);
    }

    /**
     * Store an integer setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param value the value to be set
     * @since 1.6.0
     */
    public static void setInt(SavantPlugin p, String key, int value) {
        PersistentSettings.getInstance().setInt(makePluginKey(p, key), value);
    }

    /**
     * Get a File setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @return the setting's value (or null if not found)
     * @since 2.0.0
     */
    public static File getFile(SavantPlugin p, String key) {
        return PersistentSettings.getInstance().getFile(makePluginKey(p, key));
    }

    /**
     * Store a File setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param value the value to be set
     */
    public static void setFile(SavantPlugin p, String key, File value) {
        PersistentSettings.getInstance().setFile(makePluginKey(p, key), value);
    }


    /**
     * Get a string setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @return the setting's value (or null if not found)
     */
    public static String getString(SavantPlugin p, String key) {
        return PersistentSettings.getInstance().getString(makePluginKey(p, key));
    }

    /**
     * Store a string setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param value the value to be set
     */
    public static void setString(SavantPlugin p, String key, String value) {
        PersistentSettings.getInstance().setString(makePluginKey(p, key), value);
    }

    /**
     * Get a password setting associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @return the setting's value (or null if not found)
     */
    public static String getPassword(SavantPlugin p, String key) {
        String s = PersistentSettings.getInstance().getString(makePluginKey(p, key));
        return s != null ? CryptoUtils.decrypt(s) : null;
    }

    /**
     * Store a password associated with this plugin.
     *
     * @param p the plugin making the call
     * @param key a string identifying the setting
     * @param value the value to be set
     */
    public static void setPassword(SavantPlugin p, String key, String value) {
        PersistentSettings.getInstance().setString(makePluginKey(p, key), CryptoUtils.encrypt(value));
    }


    /**
     * Flush the new settings to disk.
     */
    public static void store() {
        try {
            PersistentSettings.getInstance().store();
        } catch (IOException iox) {
            LOG.error("Unable to write settings for plugin.", iox);
        }
    }
}
