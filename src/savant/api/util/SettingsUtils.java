/*
 *    Copyright 2011 University of Toronto
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
        return p.getClass().getName() + "." + key;
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
     */
    public static String getFile(SavantPlugin p, String key) {
        return PersistentSettings.getInstance().getString(makePluginKey(p, key));
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
